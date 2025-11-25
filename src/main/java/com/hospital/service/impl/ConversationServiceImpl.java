package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.common.constant.SystemConstants;
import com.hospital.dto.request.ConversationCreateRequest;
import com.hospital.dto.request.SendMessageRequest;
import com.hospital.entity.Conversation;
import com.hospital.entity.ConversationMessage;
import com.hospital.entity.Doctor;
import com.hospital.entity.User;
import com.hospital.mapper.ConversationMapper;
import com.hospital.mapper.ConversationMessageMapper;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.UserMapper;
import com.hospital.messaging.ConversationMessagePublisher;
import com.hospital.dto.ConversationMessageEventDTO;
import com.hospital.common.constant.CacheConstants;
import com.hospital.config.AvatarConfig;
import com.hospital.service.ConversationService;
import com.hospital.service.NotificationService;
import com.hospital.service.OssService;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 医患对话服务实现
 */
@Slf4j
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {

    @Autowired
    private ConversationMessageMapper conversationMessageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private ConversationMessagePublisher conversationMessagePublisher;

    @Autowired
    private OssService ossService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AvatarConfig avatarConfig;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public IPage<Conversation> listConversations(Map<String, Object> params) {
        Page<Conversation> page = buildPage(params);
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();

        Long patientId = parseLong(params.get("patientId"));
        Long doctorId = parseLong(params.get("doctorId"));
        Long userId = parseLong(params.get("userId")); // 新增：支持通过用户ID查询
        String status = params.get("status") != null ? params.get("status").toString() : null;
        String keyword = params.get("keyword") != null ? params.get("keyword").toString() : null;

        // 确定用于缓存的用户ID（优先使用userId）
        Long cacheUserId = userId != null ? userId : (patientId != null ? patientId : doctorId);

        // 尝试从缓存获取会话列表（仅当查询条件简单时，即只有userId且无keyword和status过滤）
        boolean canUseCache = cacheUserId != null && !StringUtils.hasText(keyword) &&
                              (status == null || "ACTIVE".equals(status)) &&
                              page.getCurrent() == 1 && page.getSize() <= 20;

        if (canUseCache) {
            String listCacheKey = CacheConstants.CACHE_CONVERSATION_LIST_PREFIX + cacheUserId + ":page:1:size:" + page.getSize();
            Object cachedList = redisUtil.get(listCacheKey);
            if (cachedList != null && cachedList instanceof IPage) {
                @SuppressWarnings("unchecked")
                IPage<Conversation> cachedPage = (IPage<Conversation>) cachedList;
                return cachedPage;
            }

            // 尝试从缓存获取总数
            String countCacheKey = CacheConstants.CACHE_CONVERSATION_COUNT_PREFIX + cacheUserId;
            Object cachedCount = redisUtil.get(countCacheKey);
        }

        // 支持通过用户ID查询（新方式，支持三种身份）
        if (userId != null) {
            wrapper.and(q -> q.eq(Conversation::getParticipant1UserId, userId)
                    .or().eq(Conversation::getParticipant2UserId, userId));
            // 过滤掉被该用户删除的会话
            wrapper.and(q -> q.and(w -> w.eq(Conversation::getParticipant1UserId, userId)
                            .and(sub -> sub.isNull(Conversation::getDeletedByParticipant1)
                                    .or().eq(Conversation::getDeletedByParticipant1, 0)))
                    .or(w -> w.eq(Conversation::getParticipant2UserId, userId)
                            .and(sub -> sub.isNull(Conversation::getDeletedByParticipant2)
                                    .or().eq(Conversation::getDeletedByParticipant2, 0))));
        }

        // 向后兼容：支持通过patientId查询
        if (patientId != null && userId == null) {
            wrapper.and(q -> q.eq(Conversation::getPatientId, patientId)
                    .or().eq(Conversation::getParticipant1UserId, patientId)
                    .or().eq(Conversation::getParticipant2UserId, patientId));
            // 过滤掉被患者删除的会话
            wrapper.and(q -> q.isNull(Conversation::getDeletedByPatient)
                    .or().eq(Conversation::getDeletedByPatient, 0));
        }

        // 向后兼容：支持通过doctorId查询（可能是医生实体ID或用户ID）
        if (doctorId != null && userId == null) {
            // 先尝试作为医生实体ID查询
            wrapper.and(q -> q.eq(Conversation::getDoctorId, doctorId)
                    .or().eq(Conversation::getParticipant1UserId, doctorId)
                    .or().eq(Conversation::getParticipant2UserId, doctorId));
            // 过滤掉被医生删除的会话
            wrapper.and(q -> q.isNull(Conversation::getDeletedByDoctor)
                    .or().eq(Conversation::getDeletedByDoctor, 0));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Conversation::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(Conversation::getTitle, keyword)
                    .or().like(Conversation::getSummary, keyword));
        }

        wrapper.orderByDesc(Conversation::getUpdatedAt);
        IPage<Conversation> result = baseMapper.selectPage(page, wrapper);

        // 为会话列表中的头像生成签名URL
        List<Conversation> records = result.getRecords();
        for (Conversation conversation : records) {
            conversation.setPatientAvatar(resolveAvatarUrl(conversation.getPatientAvatar(), conversation.getPatientId(), "patient"));
            conversation.setDoctorAvatar(resolveAvatarUrl(conversation.getDoctorAvatar(), conversation.getDoctorId(), "doctor"));
            conversation.setLastSenderAvatar(resolveAvatarUrl(conversation.getLastSenderAvatar(), null, null));
        }

        // 缓存会话列表和总数（仅当查询条件简单时）
        if (canUseCache && cacheUserId != null) {
            String listCacheKey = CacheConstants.CACHE_CONVERSATION_LIST_PREFIX + cacheUserId + ":page:1:size:" + page.getSize();
            redisUtil.set(listCacheKey, result, CacheConstants.CACHE_CONVERSATION_LIST_TTL_SECONDS, TimeUnit.SECONDS);

            String countCacheKey = CacheConstants.CACHE_CONVERSATION_COUNT_PREFIX + cacheUserId;
            redisUtil.set(countCacheKey, result.getTotal(), CacheConstants.CACHE_CONVERSATION_COUNT_TTL_SECONDS, TimeUnit.SECONDS);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Conversation createConversation(ConversationCreateRequest request, Long currentUserId, Integer currentRoleType) {
        // 判断是否为管理员创建会话场景
        boolean isAdminConversation = "ADMIN_USER".equals(request.getConversationType())
                && currentRoleType != null && currentRoleType == 3;

        User patient = userMapper.selectById(request.getPatientId());
        if (patient == null) {
            throw new IllegalArgumentException("患者不存在");
        }

        Long doctorId = request.getDoctorId();
        User adminUser = null;
        Doctor doctor = null;
        User doctorUser = null;

        if (isAdminConversation) {
            // 管理员创建会话：使用当前管理员ID作为doctorId
            if (currentUserId == null) {
                throw new IllegalArgumentException("当前用户未登录");
            }
            adminUser = userMapper.selectById(currentUserId);
            if (adminUser == null || adminUser.getRoleType() == null || adminUser.getRoleType() != 3) {
                throw new IllegalArgumentException("当前用户不是管理员");
            }
            doctorId = currentUserId; // 使用管理员ID作为doctorId（向后兼容字段）
        } else {
            // 普通医患会话：需要doctorId
            if (doctorId == null) {
                throw new IllegalArgumentException("医生ID不能为空");
            }
            doctor = doctorMapper.selectById(doctorId);
            if (doctor == null) {
                throw new IllegalArgumentException("医生不存在");
            }
            doctorUser = doctor.getUserId() != null ? userMapper.selectById(doctor.getUserId()) : null;
        }

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getPatientId, request.getPatientId())
                .eq(Conversation::getDoctorId, doctorId)
                .eq(Conversation::getStatus, "ACTIVE");

        // 如果是管理员会话，还需要检查conversationType
        if (isAdminConversation) {
            wrapper.eq(Conversation::getConversationType, "ADMIN_USER");
        }

        Conversation existed = getOne(wrapper, false);
        if (existed != null) {
            // 检查删除状态
            boolean deletedByPatient = existed.getDeletedByPatient() != null && existed.getDeletedByPatient() == 1;
            boolean deletedByDoctor = existed.getDeletedByDoctor() != null && existed.getDeletedByDoctor() == 1;
            boolean deletedByParticipant1 = existed.getDeletedByParticipant1() != null && existed.getDeletedByParticipant1() == 1;
            boolean deletedByParticipant2 = existed.getDeletedByParticipant2() != null && existed.getDeletedByParticipant2() == 1;

            // 管理员会话的删除状态检查
            if (isAdminConversation) {
                // 如果会话被用户删除，则不恢复，创建新会话
                if (deletedByParticipant1) {
                    log.info("管理员会话已被用户删除，创建新会话: conversationId={}, patientId={}, adminId={}",
                            existed.getId(), request.getPatientId(), doctorId);
                }
                // 如果会话只被管理员删除，则恢复会话
                else if (deletedByParticipant2) {
                    existed.setDeletedByParticipant2(0);
                    updateById(existed);
                    evictConversationCache(existed.getId());
                    evictConversationListCache(existed);
                    log.info("恢复被管理员删除的会话: conversationId={}, patientId={}, adminId={}",
                            existed.getId(), request.getPatientId(), doctorId);
                    existed.setPatientAvatar(resolveAvatarUrl(existed.getPatientAvatar(), existed.getPatientId(),
                            getEntityTypeByRole(patient.getRoleType())));
                    existed.setDoctorAvatar(resolveAvatarUrl(existed.getDoctorAvatar(), existed.getDoctorId(), "admin"));
                    return existed;
                }
                // 如果会话未被删除，直接返回现有会话
                else {
                    existed.setPatientAvatar(resolveAvatarUrl(existed.getPatientAvatar(), existed.getPatientId(),
                            getEntityTypeByRole(patient.getRoleType())));
                    existed.setDoctorAvatar(resolveAvatarUrl(existed.getDoctorAvatar(), existed.getDoctorId(), "admin"));
                    return existed;
                }
            } else {
                // 普通医患会话的删除状态检查
                if (deletedByPatient) {
                    log.info("会话已被患者删除，创建新会话: conversationId={}, patientId={}, doctorId={}",
                            existed.getId(), request.getPatientId(), doctorId);
                }
                else if (deletedByDoctor) {
                    existed.setDeletedByDoctor(0);
                    updateById(existed);
                    evictConversationCache(existed.getId());
                    evictConversationListCache(existed);
                    log.info("恢复被医生删除的会话: conversationId={}, patientId={}, doctorId={}",
                            existed.getId(), request.getPatientId(), doctorId);
                    existed.setPatientAvatar(resolveAvatarUrl(existed.getPatientAvatar(), existed.getPatientId(), "patient"));
                    existed.setDoctorAvatar(resolveAvatarUrl(existed.getDoctorAvatar(), existed.getDoctorId(), "doctor"));
                    return existed;
                }
                else {
                    existed.setPatientAvatar(resolveAvatarUrl(existed.getPatientAvatar(), existed.getPatientId(), "patient"));
                    existed.setDoctorAvatar(resolveAvatarUrl(existed.getDoctorAvatar(), existed.getDoctorId(), "doctor"));
                    return existed;
                }
            }
        }

        Conversation conversation = new Conversation();
        conversation.setPatientId(request.getPatientId());
        conversation.setDoctorId(doctorId);

        if (isAdminConversation) {
            // 管理员会话
            conversation.setConversationType("ADMIN_USER");
            conversation.setParticipant1UserId(request.getPatientId());
            conversation.setParticipant1Role(patient.getRoleType() != null && patient.getRoleType() == 2 ? "DOCTOR" : "PATIENT");
            conversation.setParticipant2UserId(currentUserId);
            conversation.setParticipant2Role("ADMIN");

            String patientName = StringUtils.hasText(patient.getRealName()) ? patient.getRealName() : patient.getUsername();
            conversation.setPatientNickname(patientName);
            conversation.setPatientAvatar(patient.getAvatar());

            String adminName = StringUtils.hasText(adminUser.getRealName()) ? adminUser.getRealName() : adminUser.getUsername();
            conversation.setDoctorNickname(adminName != null ? adminName : "管理员");
            conversation.setDoctorAvatar(adminUser.getAvatar());
        } else {
            // 普通医患会话
            conversation.setConversationType(StringUtils.hasText(request.getConversationType()) ? request.getConversationType() : "PATIENT_DOCTOR");
            conversation.setParticipant1UserId(request.getPatientId());
            conversation.setParticipant1Role("PATIENT");
            conversation.setParticipant2UserId(doctorUser != null ? doctorUser.getId() : null);
            conversation.setParticipant2Role("DOCTOR");

            conversation.setPatientNickname(StringUtils.hasText(patient.getRealName()) ? patient.getRealName() : patient.getUsername());
            conversation.setPatientAvatar(patient.getAvatar());
            conversation.setDoctorNickname(StringUtils.hasText(doctor.getDoctorName()) ? doctor.getDoctorName() :
                    (doctorUser != null ? doctorUser.getRealName() : "医生"));
            conversation.setDoctorAvatar(doctorUser != null ? doctorUser.getAvatar() : null);
        }

        if (StringUtils.hasText(request.getTitle())) {
            conversation.setTitle(request.getTitle());
        } else {
            conversation.setTitle(conversation.getPatientNickname() + " x " + conversation.getDoctorNickname());
        }

        if (StringUtils.hasText(request.getSummary())) {
            conversation.setSummary(request.getSummary());
        } else {
            conversation.setSummary(isAdminConversation ? "与管理员对话" : "智能康复沟通会话");
        }
        conversation.setStatus("ACTIVE");
        conversation.setUnreadForDoctor(0);
        conversation.setUnreadForPatient(0);
        conversation.setUnreadForParticipant1(0);
        conversation.setUnreadForParticipant2(0);
        conversation.setDeletedByPatient(0);
        conversation.setDeletedByDoctor(0);
        conversation.setDeletedByParticipant1(0);
        conversation.setDeletedByParticipant2(0);

        save(conversation);

        // 清除会话缓存（新创建的会话）
        evictConversationCache(conversation.getId());
        // 清除相关用户的会话列表缓存（新创建的会话会影响列表）
        evictConversationListCache(conversation);

        // 为返回的会话对象生成签名头像URL
        if (isAdminConversation) {
            conversation.setPatientAvatar(resolveAvatarUrl(conversation.getPatientAvatar(), conversation.getPatientId(),
                    getEntityTypeByRole(patient.getRoleType())));
            conversation.setDoctorAvatar(resolveAvatarUrl(conversation.getDoctorAvatar(), conversation.getDoctorId(), "admin"));
        } else {
            conversation.setPatientAvatar(resolveAvatarUrl(conversation.getPatientAvatar(), conversation.getPatientId(), "patient"));
            conversation.setDoctorAvatar(resolveAvatarUrl(conversation.getDoctorAvatar(), conversation.getDoctorId(), "doctor"));
        }

        log.info("创建会话成功: conversationId={}, type={}, patientId={}, doctorId={}",
                conversation.getId(), conversation.getConversationType(), request.getPatientId(), doctorId);

        return conversation;
    }

    @Override
    public IPage<ConversationMessage> listMessages(Long conversationId, Map<String, Object> params) {
        ensureConversationExists(conversationId);
        Page<ConversationMessage> page = buildMessagePage(params);
        LambdaQueryWrapper<ConversationMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessage::getConversationId, conversationId)
                .orderByAsc(ConversationMessage::getSentAt);

        boolean canUseCache = page.getCurrent() == 1;
        String messageCacheKey = null;
        if (canUseCache) {
            messageCacheKey = redisUtil.buildCacheKey(
                    CacheConstants.CACHE_CONVERSATION_MESSAGE_LIST_PREFIX + conversationId,
                    (int) page.getCurrent(),
                    (int) page.getSize(),
                    params);
            Object cached = redisUtil.get(messageCacheKey);
            if (cached instanceof IPage) {
                @SuppressWarnings("unchecked")
                IPage<ConversationMessage> cachedPage = (IPage<ConversationMessage>) cached;
                return cachedPage;
            }
        }

        IPage<ConversationMessage> result = conversationMessageMapper.selectPage(page, wrapper);

        // 为每条消息生成可访问的头像URL（避免历史数据中的过期签名导致头像失效）
        if (result != null && result.getRecords() != null && !result.getRecords().isEmpty()) {
            for (ConversationMessage message : result.getRecords()) {
                if (message == null) {
                    continue;
                }
                String entityType = null;
                String senderRole = message.getSenderRole();
                if ("PATIENT".equalsIgnoreCase(senderRole)) {
                    entityType = "patient";
                } else if ("DOCTOR".equalsIgnoreCase(senderRole)) {
                    entityType = "doctor";
                } else if ("ADMIN".equalsIgnoreCase(senderRole)) {
                    entityType = "admin";
                }
                message.setSenderAvatar(resolveAvatarUrl(message.getSenderAvatar(), message.getSenderId(), entityType));
            }
        }

        if (canUseCache && messageCacheKey != null) {
            redisUtil.set(messageCacheKey, result,
                    CacheConstants.CACHE_CONVERSATION_MESSAGE_LIST_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationMessage appendMessage(Long conversationId, SendMessageRequest request) {
        Conversation conversation = ensureConversationExists(conversationId);

        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setSenderRole(request.getSenderRole());
        message.setSenderId(request.getSenderId());
        message.setSenderName(StringUtils.hasText(request.getSenderName())
                ? request.getSenderName()
                : defaultSenderName(request.getSenderRole(), conversation));
        String requestAvatar = StringUtils.hasText(request.getSenderAvatar())
                ? request.getSenderAvatar()
                : defaultSenderAvatar(request.getSenderRole(), conversation);
        message.setSenderAvatar(sanitizeAvatarUrl(requestAvatar));
        message.setContent(request.getContent());
        message.setContentType(StringUtils.hasText(request.getContentType()) ? request.getContentType() : "TEXT");
        message.setAttachmentUrl(StringUtils.hasText(request.getAttachmentUrl()) ? request.getAttachmentUrl() : null);
        message.setMetadata(StringUtils.hasText(request.getMetadata()) ? request.getMetadata() : null);
        message.setIsRead(0);
        message.setSentAt(LocalDateTime.now());

        conversationMessageMapper.insert(message);
        refreshConversationSnapshot(conversation, message);
        publishConversationEvent(conversation, message);
        sendMessageNotification(conversation, message);
        evictConversationMessageCache(conversationId);
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clearMessages(Long conversationId) {
        Conversation conversation = ensureConversationExists(conversationId);
        LambdaQueryWrapper<ConversationMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessage::getConversationId, conversationId);
        conversationMessageMapper.delete(wrapper);

        Conversation reset = new Conversation();
        reset.setId(conversationId);
        reset.setLastMessagePreview(null);
        reset.setLastSenderRole(null);
        reset.setLastSenderName(null);
        reset.setLastSenderAvatar(null);
        reset.setLastMessageTime(null);
        reset.setUnreadForDoctor(0);
        reset.setUnreadForPatient(0);
        reset.setSummary("暂无对话，等待新的消息");

        boolean updated = updateById(reset);
        if (updated) {
            evictConversationCache(conversationId);
            // 清除相关用户的会话列表缓存
            if (conversation != null) {
                evictConversationListCache(conversation);
            }
            evictConversationMessageCache(conversationId);
        }
        return updated;
    }

    @Override
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConversation(Long conversationId) {
        throw new IllegalArgumentException("请使用 deleteConversationByRole 方法，需要指定删除角色");
    }

    /**
     * 根据角色删除会话（单方面删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConversationByRole(Long conversationId, String role) {
        Conversation conversation = ensureConversationExists(conversationId);

        String normalizedRole = role != null ? role.toUpperCase() : "";
        Conversation update = new Conversation();
        update.setId(conversationId);

        if ("DOCTOR".equals(normalizedRole)) {
            update.setDeletedByDoctor(1);
        } else if ("PATIENT".equals(normalizedRole)) {
            update.setDeletedByPatient(1);
        } else {
            throw new IllegalArgumentException("无效的角色: " + role);
        }

        boolean updated = updateById(update);

        if (updated) {
            evictConversationCache(conversationId);
            // 清除相关用户的会话列表缓存
            // 重新获取最新状态，因为上面已经更新了删除标记
            Conversation latestConversation = getById(conversationId);
            if (latestConversation != null) {
                evictConversationListCache(latestConversation);
            }
            log.info("标记删除会话成功: conversationId={}, role={}", conversationId, role);
        } else {
            log.warn("标记删除会话失败: conversationId={}, role={}", conversationId, role);
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAllConversations(Long participantId, String role) {
        if (participantId == null) {
            throw new IllegalArgumentException("参与者ID不能为空");
        }
        if (!StringUtils.hasText(role)) {
            throw new IllegalArgumentException("角色不能为空");
        }

        String normalizedRole = role.toUpperCase();
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();

        // 根据角色查询对应的会话（不包括已被删除的）
        if ("DOCTOR".equals(normalizedRole)) {
            wrapper.eq(Conversation::getDoctorId, participantId);
            wrapper.and(q -> q.isNull(Conversation::getDeletedByDoctor)
                    .or().eq(Conversation::getDeletedByDoctor, 0));
        } else if ("PATIENT".equals(normalizedRole)) {
            wrapper.eq(Conversation::getPatientId, participantId);
            wrapper.and(q -> q.isNull(Conversation::getDeletedByPatient)
                    .or().eq(Conversation::getDeletedByPatient, 0));
        } else {
            throw new IllegalArgumentException("无效的角色: " + role);
        }

        // 查询所有要删除的会话
        List<Conversation> conversations = list(wrapper);
        if (conversations.isEmpty()) {
            log.info("没有找到要删除的会话: participantId={}, role={}", participantId, role);
            return true;
        }

        // 批量标记删除
        for (Conversation conversation : conversations) {
            Conversation update = new Conversation();
            update.setId(conversation.getId());
            if ("DOCTOR".equals(normalizedRole)) {
                update.setDeletedByDoctor(1);
            } else {
                update.setDeletedByPatient(1);
            }
            updateById(update);
            evictConversationCache(conversation.getId());
            // 清除相关用户的会话列表缓存
            evictConversationListCache(conversation);
        }

        log.info("批量标记删除会话成功: participantId={}, role={}, count={}", participantId, role, conversations.size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markConversationAsRead(Long conversationId, Long userId, String role) {
        Conversation conversation = ensureConversationExists(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("会话不存在");
        }

        Long readerUserId = resolveReaderUserId(conversation, userId, role);
        String normalizedRole = role != null ? role.toUpperCase() : null;

        if (readerUserId == null && !StringUtils.hasText(normalizedRole)) {
            throw new IllegalArgumentException("缺少有效的阅读者身份信息");
        }

        Conversation update = new Conversation();
        update.setId(conversationId);
        boolean changed = false;

        if (readerUserId != null) {
            if (Objects.equals(conversation.getParticipant1UserId(), readerUserId)
                    && Objects.requireNonNullElse(conversation.getUnreadForParticipant1(), 0) > 0) {
                update.setUnreadForParticipant1(0);
                changed = true;
            }
            if (Objects.equals(conversation.getParticipant2UserId(), readerUserId)
                    && Objects.requireNonNullElse(conversation.getUnreadForParticipant2(), 0) > 0) {
                update.setUnreadForParticipant2(0);
                changed = true;
            }
        }

        if (normalizedRole == null && readerUserId != null) {
            if (Objects.equals(conversation.getPatientId(), readerUserId)
                    || (conversation.getParticipant1Role() != null
                        && "PATIENT".equalsIgnoreCase(conversation.getParticipant1Role())
                        && Objects.equals(conversation.getParticipant1UserId(), readerUserId))) {
                normalizedRole = "PATIENT";
            } else if (Objects.equals(conversation.getDoctorId(), readerUserId)
                    || (conversation.getParticipant2Role() != null
                        && ("DOCTOR".equalsIgnoreCase(conversation.getParticipant2Role())
                            || "ADMIN".equalsIgnoreCase(conversation.getParticipant2Role()))
                        && Objects.equals(conversation.getParticipant2UserId(), readerUserId))) {
                normalizedRole = conversation.getParticipant2Role() != null
                        ? conversation.getParticipant2Role().toUpperCase()
                        : "DOCTOR";
            }
        }

        if ("PATIENT".equals(normalizedRole)) {
            if (Objects.requireNonNullElse(conversation.getUnreadForPatient(), 0) > 0) {
                update.setUnreadForPatient(0);
                changed = true;
            }
        } else if ("DOCTOR".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
            if (Objects.requireNonNullElse(conversation.getUnreadForDoctor(), 0) > 0) {
                update.setUnreadForDoctor(0);
                changed = true;
            }
        }

        if (!changed) {
            return true;
        }

        boolean updated = updateById(update);
        if (updated) {
            updateConversationCache(conversationId, update);
            evictConversationListCache(conversation);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllConversationsAsRead(Long userId, String role) {
        if (userId == null && !StringUtils.hasText(role)) {
            throw new IllegalArgumentException("缺少有效的阅读者身份信息");
        }

        String normalizedRole = role != null ? role.toUpperCase() : null;

        // 查询用户的所有未读会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();

        // 根据userId查询
        if (userId != null) {
            wrapper.and(q -> q.eq(Conversation::getParticipant1UserId, userId)
                    .or().eq(Conversation::getParticipant2UserId, userId));
            // 过滤掉被用户删除的会话
            wrapper.and(q -> q.and(w -> w.eq(Conversation::getParticipant1UserId, userId)
                            .and(sub -> sub.isNull(Conversation::getDeletedByParticipant1)
                                    .or().eq(Conversation::getDeletedByParticipant1, 0)))
                    .or(w -> w.eq(Conversation::getParticipant2UserId, userId)
                            .and(sub -> sub.isNull(Conversation::getDeletedByParticipant2)
                                    .or().eq(Conversation::getDeletedByParticipant2, 0))));
        }

        // 根据角色查询（向后兼容）
        if (normalizedRole != null) {
            if ("PATIENT".equals(normalizedRole)) {
                wrapper.and(q -> q.isNull(Conversation::getDeletedByPatient)
                        .or().eq(Conversation::getDeletedByPatient, 0));
                wrapper.and(q -> q.gt(Conversation::getUnreadForPatient, 0)
                        .or().and(w -> w.eq(Conversation::getParticipant1Role, "PATIENT")
                                .gt(Conversation::getUnreadForParticipant1, 0))
                        .or().and(w -> w.eq(Conversation::getParticipant2Role, "PATIENT")
                                .gt(Conversation::getUnreadForParticipant2, 0)));
            } else if ("DOCTOR".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
                wrapper.and(q -> q.isNull(Conversation::getDeletedByDoctor)
                        .or().eq(Conversation::getDeletedByDoctor, 0));
                wrapper.and(q -> q.gt(Conversation::getUnreadForDoctor, 0)
                        .or().and(w -> w.eq(Conversation::getParticipant1Role, normalizedRole)
                                .gt(Conversation::getUnreadForParticipant1, 0))
                        .or().and(w -> w.eq(Conversation::getParticipant2Role, normalizedRole)
                                .gt(Conversation::getUnreadForParticipant2, 0)));
            }
        }

        List<Conversation> conversations = list(wrapper);
        if (conversations.isEmpty()) {
            log.info("没有找到需要标记为已读的会话: userId={}, role={}", userId, role);
            return 0;
        }

        int count = 0;
        for (Conversation conversation : conversations) {
            try {
                boolean success = markConversationAsRead(conversation.getId(), userId, role);
                if (success) {
                    count++;
                }
            } catch (Exception e) {
                log.warn("标记会话已读失败: conversationId={}, userId={}, role={}, error={}",
                        conversation.getId(), userId, role, e.getMessage());
            }
        }

        log.info("批量标记会话已读成功: userId={}, role={}, total={}, success={}",
                userId, role, conversations.size(), count);
        return count;
    }

    private Page<Conversation> buildPage(Map<String, Object> params) {
        int page = parseInt(params.get("page"), 1);
        int pageSize = parseInt(params.get("pageSize"), SystemConstants.DEFAULT_PAGE_SIZE);
        pageSize = Math.min(pageSize, SystemConstants.MAX_PAGE_SIZE);
        return new Page<>(page, pageSize);
    }

    private Page<ConversationMessage> buildMessagePage(Map<String, Object> params) {
        int page = parseInt(params.get("page"), 1);
        int pageSize = parseInt(params.get("pageSize"), 50);
        pageSize = Math.min(pageSize, SystemConstants.MAX_PAGE_SIZE);
        return new Page<>(page, pageSize);
    }

    private Conversation ensureConversationExists(Long conversationId) {
        // 先尝试从缓存获取
        String cacheKey = CacheConstants.CACHE_CONVERSATION_PREFIX + conversationId;
        Object cached = redisUtil.get(cacheKey);
        if (cached != null && cached instanceof Conversation) {
            return (Conversation) cached;
        }

        // 缓存未命中，从数据库查询
        Conversation conversation = getById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("会话不存在");
        }

        // 存入缓存
        redisUtil.set(cacheKey, conversation, CacheConstants.CACHE_CONVERSATION_TTL_SECONDS, TimeUnit.SECONDS);

        return conversation;
    }

    /**
     * 清除会话缓存（在更新会话信息后调用）
     */
    private void evictConversationCache(Long conversationId) {
        String cacheKey = CacheConstants.CACHE_CONVERSATION_PREFIX + conversationId;
        redisUtil.delete(cacheKey);
    }

    /**
     * 更新会话缓存（方案1：更新缓存而不是清除缓存）
     * 从数据库重新查询最新的会话信息并更新缓存
     */
    private void updateConversationCache(Long conversationId, Conversation update) {
        try {
            // 重新查询最新的会话信息
            Conversation latest = getById(conversationId);
            if (latest != null) {
                String cacheKey = CacheConstants.CACHE_CONVERSATION_PREFIX + conversationId;
                redisUtil.set(cacheKey, latest, CacheConstants.CACHE_CONVERSATION_TTL_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("更新会话缓存失败，降级为清除缓存: conversationId={}, error={}", conversationId, e.getMessage());
            // 降级：如果更新失败，清除缓存
            evictConversationCache(conversationId);
        }
    }

    /**
     * 清除相关用户的会话列表缓存（方案2）
     * 当会话信息更新时，清除所有相关用户的会话列表缓存
     */
    private void evictConversationListCache(Conversation conversation) {
        try {
            // 清除participant1的会话列表缓存
            if (conversation.getParticipant1UserId() != null) {
                String pattern1 = CacheConstants.CACHE_CONVERSATION_LIST_PREFIX + conversation.getParticipant1UserId() + ":*";
                redisUtil.deleteByPattern(pattern1);
                String countKey1 = CacheConstants.CACHE_CONVERSATION_COUNT_PREFIX + conversation.getParticipant1UserId();
                redisUtil.delete(countKey1);
            }

            // 清除participant2的会话列表缓存
            if (conversation.getParticipant2UserId() != null) {
                String pattern2 = CacheConstants.CACHE_CONVERSATION_LIST_PREFIX + conversation.getParticipant2UserId() + ":*";
                redisUtil.deleteByPattern(pattern2);
                String countKey2 = CacheConstants.CACHE_CONVERSATION_COUNT_PREFIX + conversation.getParticipant2UserId();
                redisUtil.delete(countKey2);
            }

            // 向后兼容：清除patientId和doctorId的缓存
            if (conversation.getPatientId() != null) {
                String pattern3 = CacheConstants.CACHE_CONVERSATION_LIST_PREFIX + conversation.getPatientId() + ":*";
                redisUtil.deleteByPattern(pattern3);
                String countKey3 = CacheConstants.CACHE_CONVERSATION_COUNT_PREFIX + conversation.getPatientId();
                redisUtil.delete(countKey3);
            }

            if (conversation.getDoctorId() != null) {
                String pattern4 = CacheConstants.CACHE_CONVERSATION_LIST_PREFIX + conversation.getDoctorId() + ":*";
                redisUtil.deleteByPattern(pattern4);
                String countKey4 = CacheConstants.CACHE_CONVERSATION_COUNT_PREFIX + conversation.getDoctorId();
                redisUtil.delete(countKey4);
            }
        } catch (Exception e) {
            log.warn("清除会话列表缓存失败: conversationId={}, error={}", conversation.getId(), e.getMessage());
        }
    }

    private void evictConversationMessageCache(Long conversationId) {
        try {
            String pattern = CacheConstants.CACHE_CONVERSATION_MESSAGE_LIST_PREFIX + conversationId + ":*";
            redisUtil.deleteByPattern(pattern);
        } catch (Exception e) {
            log.warn("清除会话消息缓存失败: conversationId={}, error={}", conversationId, e.getMessage());
        }
    }

    private void refreshConversationSnapshot(Conversation conversation, ConversationMessage message) {
        Conversation update = new Conversation();
        update.setId(conversation.getId());
        update.setLastMessagePreview(buildPreview(message.getContent()));
        update.setLastSenderRole(message.getSenderRole());
        update.setLastSenderName(message.getSenderName());
        update.setLastSenderAvatar(message.getSenderAvatar());
        update.setLastMessageTime(message.getSentAt());

        String role = message.getSenderRole() != null ? message.getSenderRole().toUpperCase() : "SYSTEM";
        int unreadForDoctor = Objects.requireNonNullElse(conversation.getUnreadForDoctor(), 0);
        int unreadForPatient = Objects.requireNonNullElse(conversation.getUnreadForPatient(), 0);
        int unreadForParticipant1 = Objects.requireNonNullElse(conversation.getUnreadForParticipant1(), 0);
        int unreadForParticipant2 = Objects.requireNonNullElse(conversation.getUnreadForParticipant2(), 0);

        // 获取发送者的用户ID
        Long senderUserId = getSenderUserId(message, role);

        // 优先使用新字段更新未读数（只有接收者增加未读数）
        Long participant1UserId = conversation.getParticipant1UserId();
        Long participant2UserId = conversation.getParticipant2UserId();

        if (senderUserId != null && participant1UserId != null && participant2UserId != null) {
            // 使用新字段：发送者不增加未读数，只有接收者增加未读数
            if (participant1UserId.equals(senderUserId)) {
                // 发送者是participant1，接收者是participant2，增加participant2的未读数
                update.setUnreadForParticipant2(unreadForParticipant2 + 1);
                update.setUnreadForParticipant1(0); // 发送者未读数清零
            } else if (participant2UserId.equals(senderUserId)) {
                // 发送者是participant2，接收者是participant1，增加participant1的未读数
                update.setUnreadForParticipant1(unreadForParticipant1 + 1);
                update.setUnreadForParticipant2(0); // 发送者未读数清零
            }
        }

        // 向后兼容：更新旧字段的未读数
        if ("PATIENT".equals(role)) {
            update.setUnreadForDoctor(unreadForDoctor + 1);
            update.setUnreadForPatient(0);
            // 如果患者发送消息，且会话被医生删除，自动恢复会话（清除医生删除标记）
            if (conversation.getDeletedByDoctor() != null && conversation.getDeletedByDoctor() == 1) {
                update.setDeletedByDoctor(0);
                // 删除之前的所有消息（只保留新消息）
                LambdaUpdateWrapper<ConversationMessage> messageWrapper = new LambdaUpdateWrapper<>();
                messageWrapper.eq(ConversationMessage::getConversationId, conversation.getId())
                        .ne(ConversationMessage::getId, message.getId()) // 排除当前新消息
                        .eq(ConversationMessage::getDeleted, 0) // 只标记未删除的消息
                        .set(ConversationMessage::getDeleted, 1);
                conversationMessageMapper.update(null, messageWrapper);
                log.info("患者发送新消息，自动恢复被医生删除的会话并清空历史消息: conversationId={}", conversation.getId());
            }
        } else if ("DOCTOR".equals(role)) {
            update.setUnreadForPatient(unreadForPatient + 1);
            update.setUnreadForDoctor(0);
            // 如果医生发送消息，且会话被患者删除，自动恢复会话（清除患者删除标记）
            if (conversation.getDeletedByPatient() != null && conversation.getDeletedByPatient() == 1) {
                update.setDeletedByPatient(0);
                // 删除之前的所有消息（只保留新消息）
                LambdaUpdateWrapper<ConversationMessage> messageWrapper = new LambdaUpdateWrapper<>();
                messageWrapper.eq(ConversationMessage::getConversationId, conversation.getId())
                        .ne(ConversationMessage::getId, message.getId()) // 排除当前新消息
                        .eq(ConversationMessage::getDeleted, 0) // 只标记未删除的消息
                        .set(ConversationMessage::getDeleted, 1);
                conversationMessageMapper.update(null, messageWrapper);
                log.info("医生发送新消息，自动恢复被患者删除的会话并清空历史消息: conversationId={}", conversation.getId());
            }
        } else if ("ADMIN".equals(role)) {
            // 管理员发送消息，更新接收者的未读数（向后兼容）
            if (participant1UserId != null && participant1UserId.equals(senderUserId)) {
                update.setUnreadForParticipant2(unreadForParticipant2 + 1);
                update.setUnreadForParticipant1(0);
            } else if (participant2UserId != null && participant2UserId.equals(senderUserId)) {
                update.setUnreadForParticipant1(unreadForParticipant1 + 1);
                update.setUnreadForParticipant2(0);
            }
        }

        updateById(update);

        // 方案1：更新缓存而不是清除缓存
        updateConversationCache(conversation.getId(), update);

        // 方案2：清除相关用户的会话列表缓存（因为会话列表已更新）
        evictConversationListCache(conversation);
    }

    private void publishConversationEvent(Conversation conversation, ConversationMessage message) {
        try {
            Long doctorUserId = null;
            Long participant1UserId = conversation.getParticipant1UserId();
            Long participant2UserId = conversation.getParticipant2UserId();

            // 优先使用新字段
            if (participant1UserId != null && participant2UserId != null) {
                // 根据对话类型和参与者角色确定doctorUserId
                if ("PATIENT_DOCTOR".equals(conversation.getConversationType())) {
                    // 患者-医生对话：participant2是医生
                    if ("DOCTOR".equals(conversation.getParticipant2Role())) {
                        doctorUserId = participant2UserId;
                    }
                } else if ("ADMIN_USER".equals(conversation.getConversationType())) {
                    // 管理员-用户对话：participant2是管理员
                    if ("ADMIN".equals(conversation.getParticipant2Role())) {
                        doctorUserId = participant2UserId;
                    }
                }
            }

            // 向后兼容：如果新字段没有值，使用旧逻辑
            if (doctorUserId == null && conversation.getDoctorId() != null) {
                Doctor doctor = doctorMapper.selectById(conversation.getDoctorId());
                if (doctor != null) {
                    // 找到医生记录，使用医生的用户ID
                    doctorUserId = doctor.getUserId();
                } else {
                    // 如果找不到医生记录，可能是管理员对话（doctorId存储的是管理员用户ID）
                    // 检查是否是管理员用户
                    User user = userMapper.selectById(conversation.getDoctorId());
                    if (user != null && user.getRoleType() != null && user.getRoleType() == 3) {
                        // 这是管理员，doctorId就是管理员用户ID
                        doctorUserId = conversation.getDoctorId();
                    } else {
                        // 如果既不是医生也不是管理员，但消息发送者是管理员，则使用senderId
                        // 这种情况可能发生在管理员对话中，doctorId可能存储的是管理员用户ID
                        if ("ADMIN".equals(message.getSenderRole())) {
                            // 管理员发送消息时，senderId就是管理员用户ID
                            doctorUserId = message.getSenderId();
                        }
                    }
                }
            }

            String entityType = null;
            String senderRole = message.getSenderRole();
            if ("PATIENT".equalsIgnoreCase(senderRole)) {
                entityType = "patient";
            } else if ("DOCTOR".equalsIgnoreCase(senderRole)) {
                entityType = "doctor";
            } else if ("ADMIN".equalsIgnoreCase(senderRole)) {
                entityType = "admin";
            }
            String resolvedSenderAvatar = resolveAvatarUrl(message.getSenderAvatar(), message.getSenderId(), entityType);

            ConversationMessageEventDTO event = ConversationMessageEventDTO.builder()
                    .conversationId(conversation.getId())
                    .messageId(message.getId())
                    .patientId(conversation.getPatientId()) // 向后兼容
                    .doctorId(conversation.getDoctorId()) // 向后兼容
                    .doctorUserId(doctorUserId) // 向后兼容
                    .conversationType(conversation.getConversationType())
                    .participant1UserId(conversation.getParticipant1UserId())
                    .participant1Role(conversation.getParticipant1Role())
                    .participant2UserId(conversation.getParticipant2UserId())
                    .participant2Role(conversation.getParticipant2Role())
                    .senderId(message.getSenderId())
                    .senderRole(message.getSenderRole())
                    .senderName(message.getSenderName())
                    .senderAvatar(resolvedSenderAvatar)
                    .content(message.getContent())
                    .contentType(message.getContentType())
                    .sentAt(message.getSentAt())
                    .lastMessagePreview(buildPreview(message.getContent()))
                    .build();

            conversationMessagePublisher.publish(event);
        } catch (Exception e) {
            log.error("会话消息事件发布失败：conversationId={}, messageId={}",
                    conversation.getId(), message.getId(), e);
        }
    }

    private String buildPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String trimmed = content.trim();
        return trimmed.length() > 120 ? trimmed.substring(0, 117) + "..." : trimmed;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法解析为Long：{}", value);
            return null;
        }
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法解析为int：{}", value);
            return defaultValue;
        }
    }

    private String defaultSenderName(String role, Conversation conversation) {
        String normalized = role != null ? role.toUpperCase() : "";
        if ("DOCTOR".equals(normalized)) {
            return conversation.getDoctorNickname();
        }
        if ("PATIENT".equals(normalized)) {
            return conversation.getPatientNickname();
        }
        return "系统助手";
    }

    private String defaultSenderAvatar(String role, Conversation conversation) {
        String normalized = role != null ? role.toUpperCase() : "";
        if ("DOCTOR".equals(normalized)) {
            return conversation.getDoctorAvatar();
        }
        if ("PATIENT".equals(normalized)) {
            return conversation.getPatientAvatar();
        }
        return null;
    }

    /**
     * 去除头像URL中的过期签名参数，确保可以重新生成新的签名URL
     */
    private String sanitizeAvatarUrl(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return null;
        }
        String sanitized = avatarUrl.trim();
        boolean containsSignatureParam = sanitized.contains("Signature=") || sanitized.contains("OSSAccessKeyId=")
                || sanitized.contains("Expires=");
        if (containsSignatureParam) {
            int questionIndex = sanitized.indexOf('?');
            if (questionIndex > 0) {
                sanitized = sanitized.substring(0, questionIndex);
            } else {
                int ampIndex = sanitized.indexOf('&');
                if (ampIndex > 0) {
                    sanitized = sanitized.substring(0, ampIndex);
                }
            }
        }
        return sanitized;
    }

    /**
     * 发送消息通知提醒
     * 根据新表结构，只有接收信息的人才会收到未读消息提醒
     */
    private void sendMessageNotification(Conversation conversation, ConversationMessage message) {
        try {
            String senderRole = message.getSenderRole() != null ? message.getSenderRole().toUpperCase() : "";
            String senderName = message.getSenderName() != null ? message.getSenderName() : "未知用户";
            String preview = buildPreview(message.getContent());

            // 获取发送者的用户ID
            Long senderUserId = getSenderUserId(message, senderRole);
            if (senderUserId == null) {
                log.warn("无法获取发送者用户ID，跳过通知：conversationId={}, messageId={}, senderRole={}, senderId={}",
                        conversation.getId(), message.getId(), senderRole, message.getSenderId());
                return;
            }

            // 优先使用新字段确定接收方用户ID
            Long receiverUserId = null;
            String receiverName = "";

            Long participant1UserId = conversation.getParticipant1UserId();
            Long participant2UserId = conversation.getParticipant2UserId();
            String conversationType = conversation.getConversationType();

            // 使用新字段判断接收者
            if (participant1UserId != null && participant2UserId != null) {
                if (participant1UserId.equals(senderUserId)) {
                    // 发送者是participant1，接收者是participant2
                    receiverUserId = participant2UserId;
                    // 根据对话类型和参与者角色确定接收者名称
                    if ("PATIENT_DOCTOR".equals(conversationType)) {
                        receiverName = conversation.getDoctorNickname() != null
                                ? conversation.getDoctorNickname() : "医生";
                    } else if ("ADMIN_USER".equals(conversationType)) {
                        receiverName = conversation.getDoctorNickname() != null
                                ? conversation.getDoctorNickname() : "管理员";
                    }
                } else if (participant2UserId.equals(senderUserId)) {
                    // 发送者是participant2，接收者是participant1
                    receiverUserId = participant1UserId;
                    receiverName = conversation.getPatientNickname() != null
                            ? conversation.getPatientNickname() : "用户";
                } else {
                    log.warn("发送者用户ID与participant不匹配：senderUserId={}, participant1UserId={}, participant2UserId={}",
                            senderUserId, participant1UserId, participant2UserId);
                }
            }

            // 向后兼容：如果新字段没有值，使用旧逻辑
            if (receiverUserId == null) {

                // 判断是否是管理员对话
                boolean isAdminConversation = "ADMIN_USER".equals(conversationType) ||
                        (conversation.getDoctorId() != null && conversation.getDoctorId().equals(participant2UserId));

                if ("PATIENT".equals(senderRole)) {
                    // 患者发送，通知医生或管理员
                    if (isAdminConversation) {
                        // 管理员对话：通知管理员（participant2或doctorId）
                        receiverUserId = participant2UserId != null ? participant2UserId : conversation.getDoctorId();
                        receiverName = conversation.getDoctorNickname() != null
                                ? conversation.getDoctorNickname() : "管理员";
                    } else {
                        // 普通对话：通知医生
                        if (conversation.getDoctorId() != null) {
                            Doctor doctor = doctorMapper.selectById(conversation.getDoctorId());
                            if (doctor != null && doctor.getUserId() != null) {
                                receiverUserId = doctor.getUserId();
                                receiverName = conversation.getDoctorNickname() != null
                                        ? conversation.getDoctorNickname() : "医生";
                            }
                        }
                    }
                } else if ("DOCTOR".equals(senderRole)) {
                    // 医生发送，需要区分管理员对话和普通对话
                    if (isAdminConversation) {
                        // 管理员对话：通知管理员（participant2或doctorId）
                        receiverUserId = participant2UserId != null ? participant2UserId : conversation.getDoctorId();
                        receiverName = conversation.getDoctorNickname() != null
                                ? conversation.getDoctorNickname() : "管理员";
                    } else {
                        // 普通对话：通知患者
                        if (conversation.getPatientId() != null) {
                            receiverUserId = conversation.getPatientId();
                            receiverName = conversation.getPatientNickname() != null
                                    ? conversation.getPatientNickname() : "患者";
                        }
                    }
                } else if ("ADMIN".equals(senderRole)) {
                    // 管理员发送，通知用户（participant1）
                    if (participant1UserId != null) {
                        receiverUserId = participant1UserId;
                        receiverName = conversation.getPatientNickname() != null
                                ? conversation.getPatientNickname() : "用户";
                    }
                }
            }

            // 检查发送者和接收者是否是同一用户，如果是则不发送通知（避免自己给自己发送通知）
            if (receiverUserId != null && senderUserId.equals(receiverUserId)) {
                return;
            }

            // 只有接收者才会收到通知
            if (receiverUserId != null) {
                String title = String.format("%s发来新消息", senderName);
                String content = String.format("%s：%s", senderName, preview);
                
                // 判断通知类型：医生回复患者时使用DOCTOR_REPLY，其他情况使用CONVERSATION_MESSAGE
                String notificationType = "CONVERSATION_MESSAGE";
                if ("DOCTOR".equals(senderRole)) {
                    // 判断是否是管理员对话
                    boolean isAdminConversation = "ADMIN_USER".equals(conversation.getConversationType()) ||
                            (conversation.getDoctorId() != null && conversation.getDoctorId().equals(participant2UserId));
                    // 如果不是管理员对话，且接收者是患者，则使用DOCTOR_REPLY类型
                    if (!isAdminConversation && conversation.getPatientId() != null && conversation.getPatientId().equals(receiverUserId)) {
                        notificationType = "DOCTOR_REPLY";
                    }
                }
                
                notificationService.createAndSendNotification(
                        receiverUserId,
                        title,
                        content,
                        notificationType
                );
                log.info("已发送对话消息通知：receiverUserId={}, senderUserId={}, senderName={}, conversationId={}, type={}",
                        receiverUserId, senderUserId, senderName, conversation.getId(), notificationType);
            } else {
                log.warn("无法确定接收者，跳过通知：conversationId={}, messageId={}, senderRole={}",
                        conversation.getId(), message.getId(), senderRole);
            }
        } catch (Exception e) {
            log.error("发送对话消息通知失败：conversationId={}, messageId={}, error={}",
                    conversation.getId(), message.getId(), e.getMessage(), e);
        }
    }

    /**
     * 获取发送者的用户ID
     */
    private Long getSenderUserId(ConversationMessage message, String senderRole) {
        if ("PATIENT".equals(senderRole) || "ADMIN".equals(senderRole)) {
            // 患者和管理员：senderId就是用户ID
            return message.getSenderId();
        } else if ("DOCTOR".equals(senderRole)) {
            // 医生发送：需要判断senderId是实体ID还是用户ID
            if (message.getSenderId() != null) {
                // 先尝试从缓存获取医生信息
                String cacheKey = CacheConstants.CACHE_DOCTOR_PREFIX + message.getSenderId();
                Object cached = redisUtil.get(cacheKey);
                Doctor senderDoctor = null;

                if (cached != null && cached instanceof Doctor) {
                    senderDoctor = (Doctor) cached;
                } else {
                    // 缓存未命中，从数据库查询
                    senderDoctor = doctorMapper.selectById(message.getSenderId());
                    if (senderDoctor != null) {
                        // 存入缓存
                        redisUtil.set(cacheKey, senderDoctor, CacheConstants.CACHE_DOCTOR_TTL_SECONDS, TimeUnit.SECONDS);
                    }
                }

                if (senderDoctor != null && senderDoctor.getUserId() != null) {
                    // 找到了医生记录，说明senderId是实体ID，返回用户ID
                    return senderDoctor.getUserId();
                }
                // 如果没有找到医生记录，可能是管理员对话中，senderId已经是用户ID
                // 或者senderId本身就是用户ID（前端在管理员对话中发送的是用户ID）
                // 在这种情况下，直接返回senderId作为用户ID
                return message.getSenderId();
            }
        }
        return null;
    }

    /**
     * 生成可直接访问的头像URL
     */
    private String resolveAvatarUrl(String rawAvatar, Long entityId, String entityType) {
        String sanitizedAvatar = sanitizeAvatarUrl(rawAvatar);
        if (StringUtils.hasText(sanitizedAvatar)) {
            // 尝试从缓存获取签名URL
            String cacheKey = CacheConstants.CACHE_OSS_SIGNED_URL_PREFIX + sanitizedAvatar;
            Object cached = redisUtil.get(cacheKey);
            if (cached != null && cached instanceof String) {
                return (String) cached;
            }

            try {
                String signedUrl = ossService.generatePresignedUrl(sanitizedAvatar, avatarConfig.getTtlMinutes());
                if (StringUtils.hasText(signedUrl)) {
                    // 存入缓存
                    redisUtil.set(cacheKey, signedUrl, CacheConstants.CACHE_OSS_SIGNED_URL_TTL_SECONDS, TimeUnit.SECONDS);
                    return signedUrl;
                }
            } catch (Exception e) {
                log.warn("生成头像签名URL失败: avatar={}, error={}", sanitizedAvatar, e.getMessage());
                return sanitizedAvatar;
            }
        }
        // 使用默认头像
        if ("patient".equals(entityType) && entityId != null) {
            return avatarConfig.getDefaultPatient() + "&seed=" + entityId;
        }
        if ("doctor".equals(entityType) && entityId != null) {
            return avatarConfig.getDefaultDoctor() + "&seed=" + entityId;
        }
        if ("admin".equals(entityType) && entityId != null) {
            return avatarConfig.getDefaultAdmin() + "&seed=" + entityId;
        }
        return avatarConfig.getDefaultPatient();
    }

    @Override
    public List<Map<String, Object>> getRecentSenders(Long conversationId) {
        ensureConversationExists(conversationId);

        // 查询最近的消息，按发送时间倒序，获取不同的发送者
        LambdaQueryWrapper<ConversationMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessage::getConversationId, conversationId)
                .eq(ConversationMessage::getDeleted, 0)
                .orderByDesc(ConversationMessage::getSentAt)
                .last("LIMIT 100"); // 先取最近100条消息，然后去重

        List<ConversationMessage> messages = conversationMessageMapper.selectList(wrapper);

        // 去重，保留每个发送者的最新一条消息
        Map<String, ConversationMessage> uniqueSenders = new LinkedHashMap<>();
        for (ConversationMessage message : messages) {
            String senderKey = message.getSenderId() + "_" + message.getSenderRole();
            if (!uniqueSenders.containsKey(senderKey)) {
                uniqueSenders.put(senderKey, message);
            }
        }

        // 取前三个，转换为Map列表
        List<Map<String, Object>> result = new ArrayList<>();
        int count = 0;
        for (ConversationMessage message : uniqueSenders.values()) {
            if (count >= 3) break;

            Map<String, Object> senderInfo = new HashMap<>();
            senderInfo.put("senderId", message.getSenderId());
            senderInfo.put("senderRole", message.getSenderRole());
            senderInfo.put("senderName", message.getSenderName());

            String entityType = null;
            if ("PATIENT".equals(message.getSenderRole())) {
                entityType = "patient";
            } else if ("DOCTOR".equals(message.getSenderRole())) {
                entityType = "doctor";
            } else if ("ADMIN".equals(message.getSenderRole())) {
                entityType = "admin";
            }
            senderInfo.put("senderAvatar", resolveAvatarUrl(message.getSenderAvatar(), message.getSenderId(), entityType));

            result.add(senderInfo);
            count++;
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Conversation getOrCreateAdminConversation(Long senderId) {
        if (senderId == null) {
            throw new IllegalArgumentException("发送者ID不能为空");
        }

        // 查找一个管理员用户（roleType=3）
        LambdaQueryWrapper<User> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(User::getRoleType, 3)
                .eq(User::getStatus, 1)
                .last("LIMIT 1");
        User admin = userMapper.selectOne(adminWrapper);

        if (admin == null) {
            throw new IllegalArgumentException("系统中没有可用的管理员");
        }

        // 查询发送者用户信息
        User sender = userMapper.selectById(senderId);
        if (sender == null) {
            throw new IllegalArgumentException("发送者不存在");
        }

        // 查询是否已存在该发送者与管理员对话的会话
        // 使用patientId存储发送者ID，doctorId存储管理员ID（虽然字段名不太合适，但可以工作）
        // 为了区分管理员对话，我们可以使用一个特殊的标记，比如doctorId为-1或使用admin的ID
        // 这里我们使用admin的ID作为doctorId，但需要确保不会与真实的医生ID冲突
        // 更好的方式是添加一个conversation_type字段，但为了快速实现，我们使用现有字段

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getPatientId, senderId)
                .eq(Conversation::getDoctorId, admin.getId()) // 使用管理员ID作为doctorId
                .eq(Conversation::getStatus, "ACTIVE");
        Conversation existed = getOne(wrapper, false);

        if (existed != null) {
            // 如果会话存在但被发送者删除，恢复它
            if (existed.getDeletedByPatient() != null && existed.getDeletedByPatient() == 1) {
                existed.setDeletedByPatient(0);
                updateById(existed);
                evictConversationCache(existed.getId());
                evictConversationListCache(existed);
            }
            // 为已存在的会话对象生成签名头像URL
            existed.setPatientAvatar(resolveAvatarUrl(existed.getPatientAvatar(), existed.getPatientId(),
                    getEntityTypeByRole(sender.getRoleType())));
            existed.setDoctorAvatar(resolveAvatarUrl(existed.getDoctorAvatar(), existed.getDoctorId(), "admin"));
            return existed;
        }

        // 创建新的管理员对话会话
        Conversation conversation = new Conversation();
        conversation.setPatientId(senderId); // 发送者ID（向后兼容）
        conversation.setDoctorId(admin.getId()); // 管理员ID（向后兼容）
        conversation.setConversationType("ADMIN_USER");
        conversation.setParticipant1UserId(senderId);
        conversation.setParticipant1Role(sender.getRoleType() != null && sender.getRoleType() == 2 ? "DOCTOR" : "PATIENT");
        conversation.setParticipant2UserId(admin.getId());
        conversation.setParticipant2Role("ADMIN");

        // 设置发送者信息
        String senderName = StringUtils.hasText(sender.getRealName()) ? sender.getRealName() : sender.getUsername();
        conversation.setPatientNickname(senderName);
        conversation.setPatientAvatar(sender.getAvatar());

        // 设置管理员信息
        String adminName = StringUtils.hasText(admin.getRealName()) ? admin.getRealName() : admin.getUsername();
        conversation.setDoctorNickname(adminName != null ? adminName : "管理员");
        conversation.setDoctorAvatar(admin.getAvatar());

        // 设置标题和摘要
        conversation.setTitle(senderName + " x 管理员");
        conversation.setSummary("与管理员对话");

        conversation.setStatus("ACTIVE");
        conversation.setUnreadForDoctor(0);
        conversation.setUnreadForPatient(0);
        conversation.setUnreadForParticipant1(0);
        conversation.setUnreadForParticipant2(0);
        conversation.setDeletedByPatient(0);
        conversation.setDeletedByDoctor(0);
        conversation.setDeletedByParticipant1(0);
        conversation.setDeletedByParticipant2(0);

        save(conversation);

        // 清除会话缓存（新创建的会话）
        evictConversationCache(conversation.getId());
        // 清除相关用户的会话列表缓存（新创建的会话会影响列表）
        evictConversationListCache(conversation);

        // 为返回的会话对象生成签名头像URL
        conversation.setPatientAvatar(resolveAvatarUrl(conversation.getPatientAvatar(), conversation.getPatientId(),
                getEntityTypeByRole(sender.getRoleType())));
        conversation.setDoctorAvatar(resolveAvatarUrl(conversation.getDoctorAvatar(), conversation.getDoctorId(), "admin"));

        log.info("创建管理员对话会话成功: conversationId={}, senderId={}, adminId={}",
                conversation.getId(), senderId, admin.getId());

        return conversation;
    }

    /**
     * 根据角色类型获取实体类型（用于头像解析）
     */
    private String getEntityTypeByRole(Integer roleType) {
        if (roleType == null) {
            return "patient";
        }
        if (roleType == 1) {
            return "patient";
        }
        if (roleType == 2) {
            return "doctor";
        }
        if (roleType == 3) {
            return "admin";
        }
        return "patient";
    }

    /**
     * 根据传入的参数解析出阅读者的用户ID
     */
    private Long resolveReaderUserId(Conversation conversation, Long userId, String role) {
        if (userId != null) {
            return userId;
        }

        String normalizedRole = role != null ? role.toUpperCase() : null;
        if ("PATIENT".equals(normalizedRole)) {
            if (conversation.getParticipant1Role() != null
                    && "PATIENT".equalsIgnoreCase(conversation.getParticipant1Role())
                    && conversation.getParticipant1UserId() != null) {
                return conversation.getParticipant1UserId();
            }
            return conversation.getPatientId();
        }

        if ("DOCTOR".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
            if (conversation.getParticipant1Role() != null
                    && normalizedRole.equalsIgnoreCase(conversation.getParticipant1Role())
                    && conversation.getParticipant1UserId() != null) {
                return conversation.getParticipant1UserId();
            }
            if (conversation.getParticipant2Role() != null
                    && normalizedRole.equalsIgnoreCase(conversation.getParticipant2Role())
                    && conversation.getParticipant2UserId() != null) {
                return conversation.getParticipant2UserId();
            }
            return conversation.getDoctorId();
        }

        return userId;
    }
}


