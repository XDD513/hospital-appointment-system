package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.constant.CacheConstants;
import com.hospital.common.constant.SystemConstants;
import com.hospital.config.SystemSettingManager;
import com.hospital.config.UserConfig;
import com.hospital.dto.OperationLogExportDTO;
import com.hospital.common.constant.AppointmentStatus;
import com.hospital.common.result.ResultCode;
import com.hospital.entity.Appointment;
import com.hospital.entity.ConsultationRecord;
import com.hospital.entity.Dictionary;
import com.hospital.entity.Doctor;
import com.hospital.entity.OperationLog;
import com.hospital.entity.SystemConfig;
import com.hospital.entity.User;
import com.hospital.mapper.AppointmentMapper;
import com.hospital.mapper.ConsultationRecordMapper;
import com.hospital.mapper.DictionaryMapper;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.OperationLogMapper;
import com.hospital.mapper.SystemConfigMapper;
import com.hospital.mapper.UserMapper;
import com.hospital.service.SystemService;
import com.hospital.service.SystemSettingNacosSyncService;
import com.alibaba.excel.EasyExcel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统管理服务实现类
 */
@Slf4j
@Service
public class SystemServiceImpl implements SystemService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private DictionaryMapper dictionaryMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private ConsultationRecordMapper consultationRecordMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserConfig userConfig;

    @Autowired
    private com.hospital.util.RedisUtil redisUtil;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private SystemSettingNacosSyncService systemSettingNacosSyncService;
    
    @Autowired
    private com.hospital.service.OssService ossService;
    
    @Autowired
    private com.hospital.config.AvatarConfig avatarConfig;

    // ==================== 操作日志管理 ====================

    @Override
    public IPage<OperationLog> getOperationLogs(Map<String, Object> params) {
        log.info("获取操作日志列表，参数：{}", params);

        // 安全地获取分页参数
        Integer page = 1;
        Integer pageSize = SystemConstants.DEFAULT_PAGE_SIZE;

        if (params.get("page") != null) {
            try {
                page = Integer.parseInt(params.get("page").toString());
            } catch (NumberFormatException e) {
                log.warn("无效的页码参数：{}", params.get("page"));
            }
        }

        if (params.get("pageSize") != null) {
            try {
                pageSize = Integer.parseInt(params.get("pageSize").toString());
            } catch (NumberFormatException e) {
                log.warn("无效的页面大小参数：{}", params.get("pageSize"));
            }
        }

        QueryWrapper<OperationLog> wrapper = new QueryWrapper<>();

        boolean hasKeyword = false;
        if (params.containsKey("keyword") && StringUtils.hasText((String) params.get("keyword"))) {
            String keyword = (String) params.get("keyword");
            hasKeyword = true;
            wrapper.and(w ->
                w.like("username", keyword)
                 .or()
                 .like("operation_module", keyword)
                 .or()
                 .like("operation_desc", keyword)
            );
        }

        // 用户名筛选（兼容旧参数，当未使用关键字查询时才使用）
        if (!hasKeyword && params.containsKey("username") && StringUtils.hasText((String) params.get("username"))) {
            wrapper.like("username", params.get("username"));
        }

        // 操作模块筛选
        if (params.containsKey("operationModule") && StringUtils.hasText((String) params.get("operationModule"))) {
            wrapper.eq("operation_module", params.get("operationModule"));
        }

        // 操作类型筛选
        if (params.containsKey("operationType") && StringUtils.hasText((String) params.get("operationType"))) {
            wrapper.eq("operation_type", params.get("operationType"));
        }

        // 状态筛选
        if (params.containsKey("status") && params.get("status") != null) {
            wrapper.eq("status", params.get("status"));
        }

        // 时间范围筛选
        if (params.containsKey("startDate") && StringUtils.hasText((String) params.get("startDate"))) {
            wrapper.ge("created_at", params.get("startDate"));
        }
        if (params.containsKey("endDate") && StringUtils.hasText((String) params.get("endDate"))) {
            wrapper.le("created_at", params.get("endDate"));
        }

        wrapper.orderByDesc("created_at");

        // 构造热门分页缓存（仅前2页），使用参数哈希简化层级
        Map<String, Object> filterParams = new java.util.HashMap<>();
        if (params.containsKey("keyword") && StringUtils.hasText((String) params.get("keyword"))) {
            filterParams.put("keyword", params.get("keyword"));
        } else if (params.containsKey("username") && StringUtils.hasText((String) params.get("username"))) {
            filterParams.put("user", params.get("username"));
        }
        if (params.containsKey("operationModule") && StringUtils.hasText((String) params.get("operationModule"))) {
            filterParams.put("module", params.get("operationModule"));
        }
        if (params.containsKey("operationType") && StringUtils.hasText((String) params.get("operationType"))) {
            filterParams.put("type", params.get("operationType"));
        }
        if (params.containsKey("status") && params.get("status") != null) {
            filterParams.put("status", params.get("status"));
        }
        if (params.containsKey("startDate") && StringUtils.hasText((String) params.get("startDate"))) {
            filterParams.put("start", params.get("startDate"));
        }
        if (params.containsKey("endDate") && StringUtils.hasText((String) params.get("endDate"))) {
            filterParams.put("end", params.get("endDate"));
        }

        String cacheKey = redisUtil.buildCacheKey("hospital:admin:oplog:list", page, pageSize, filterParams);

        if (page <= 2) {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                IPage<OperationLog> cachedPage = (IPage<OperationLog>) cached;
                return cachedPage;
            }
        }

        Page<OperationLog> pageObject = new Page<>(page, pageSize);
        IPage<OperationLog> result = operationLogMapper.selectPage(pageObject, wrapper);
        if (page <= 2) {
            redisUtil.set(cacheKey, result, CacheConstants.OPLOG_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return result;
    }

    @Override
    public void recordOperationLog(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
        // 新增日志后，热门分页缓存失效（匹配新的:pX:sY:hZ 键格式）
        try {
            redisUtil.deleteByPattern("hospital:admin:oplog:list:*");
        } catch (Exception ignored) {}
    }

    @Override
    public byte[] exportOperationLogs(Map<String, Object> params) {
        log.info("导出操作日志，参数：{}", params);

        try {
            // 构建查询条件（不分页，获取所有符合条件的数据）
            QueryWrapper<OperationLog> wrapper = new QueryWrapper<>();

            boolean hasKeyword = false;
            if (params.containsKey("keyword") && StringUtils.hasText((String) params.get("keyword"))) {
                String keyword = (String) params.get("keyword");
                hasKeyword = true;
                wrapper.and(w ->
                    w.like("username", keyword)
                     .or()
                     .like("operation_module", keyword)
                     .or()
                     .like("operation_desc", keyword)
                );
            }

            // 用户名筛选（兼容旧参数，当未使用关键字查询时才使用）
            if (!hasKeyword && params.containsKey("username") && StringUtils.hasText((String) params.get("username"))) {
                wrapper.like("username", params.get("username"));
            }

            // 操作模块筛选
            if (params.containsKey("operationModule") && StringUtils.hasText((String) params.get("operationModule"))) {
                wrapper.eq("operation_module", params.get("operationModule"));
            }

            // 操作类型筛选
            if (params.containsKey("operationType") && StringUtils.hasText((String) params.get("operationType"))) {
                wrapper.eq("operation_type", params.get("operationType"));
            }

            // 状态筛选
            if (params.containsKey("status") && params.get("status") != null) {
                wrapper.eq("status", params.get("status"));
            }

            // 时间范围筛选
            if (params.containsKey("startDate") && StringUtils.hasText((String) params.get("startDate"))) {
                wrapper.ge("created_at", params.get("startDate"));
            }
            if (params.containsKey("endDate") && StringUtils.hasText((String) params.get("endDate"))) {
                wrapper.le("created_at", params.get("endDate"));
            }

            wrapper.orderByDesc("created_at");

            // 查询所有符合条件的日志（限制最多10000条）
            Page<OperationLog> page = new Page<>(1, 10000);
            IPage<OperationLog> result = operationLogMapper.selectPage(page, wrapper);
            List<OperationLog> logs = result.getRecords();

            // 转换为导出DTO
            List<OperationLogExportDTO> exportData = logs.stream()
                .map(log -> {
                    OperationLogExportDTO dto = new OperationLogExportDTO();
                    dto.setUsername(log.getUsername());
                    dto.setOperationModule(log.getOperationModule());
                    dto.setOperationType(log.getOperationType());
                    dto.setOperationDesc(log.getOperationDesc());
                    dto.setRequestMethod(log.getRequestMethod());
                    dto.setRequestUrl(log.getRequestUrl());
                    dto.setIpAddress(log.getIpAddress());
                    dto.setExecutionTime(log.getExecutionTime());
                    dto.setStatus(log.getStatus() == 1 ? "成功" : "失败");
                    dto.setCreatedAt(log.getCreatedAt() != null ? log.getCreatedAt().toString() : "");
                    return dto;
                })
                .collect(Collectors.toList());

            // 使用EasyExcel导出
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            EasyExcel.write(outputStream, OperationLogExportDTO.class)
                .sheet("操作日志")
                .doWrite(exportData);

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("导出操作日志失败", e);
            throw new RuntimeException("导出操作日志失败: " + e.getMessage());
        }
    }

    // ==================== 系统配置管理 ====================

    @Override
    public List<SystemConfig> getSystemSettings() {
        log.info("获取系统设置");
        String cacheKey = "hospital:admin:config:list";
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<SystemConfig> list = (List<SystemConfig>) cached;
                return list;
            } catch (ClassCastException ignored) {}
        }

        QueryWrapper<SystemConfig> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("config_type");
        List<SystemConfig> settings = systemConfigMapper.selectList(wrapper);
        // TTL为-1表示永不过期
        redisUtil.set(cacheKey, settings);
        return settings;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSystemSettings(List<SystemConfig> configs) {
        log.info("更新系统设置，配置数量：{}", configs.size());

        for (SystemConfig config : configs) {
            QueryWrapper<SystemConfig> wrapper = new QueryWrapper<>();
            wrapper.eq("config_key", config.getConfigKey());
            SystemConfig existingConfig = systemConfigMapper.selectOne(wrapper);

            if (existingConfig != null) {
                existingConfig.setConfigValue(config.getConfigValue());
                existingConfig.setConfigType(config.getConfigType());
                systemConfigMapper.updateById(existingConfig);
                log.info("更新配置：{} = {}", config.getConfigKey(), config.getConfigValue());
            } else {
                config.setCreatedAt(java.time.LocalDateTime.now());
                config.setUpdatedAt(java.time.LocalDateTime.now());
                int inserted = systemConfigMapper.insert(config);
                log.info("新增配置：{} = {} (影响行数: {})",
                        config.getConfigKey(), config.getConfigValue(), inserted);
            }
        }

        // 设置更新后失效系统设置缓存及按键缓存
        try {
            redisUtil.delete("hospital:admin:config:list");
            redisUtil.deleteByPattern(CacheConstants.SYSTEM_CONFIG_CACHE_PREFIX + "*");
        } catch (Exception e) {
            log.warn("清理系统配置缓存失败", e);
        }
        try {
            systemSettingManager.refresh();
        } catch (Exception e) {
            log.warn("刷新系统配置缓存失败", e);
        }
        systemSettingNacosSyncService.sync(systemSettingManager.snapshot());
        return true;
    }

    @Override
    public String getConfigValue(String key) {
        String value = systemSettingManager.getString(key, null);
        if (value != null) {
            return value;
        }

        // 兼容旧逻辑：若内存缓存未加载到该key，再查一次数据库并刷新缓存
        QueryWrapper<SystemConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_key", key);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);
        if (config != null) {
            systemSettingManager.refresh();
            return config.getConfigValue();
        }
        return null;
    }

    // ==================== 数据字典管理 ====================

    @Override
    public List<Dictionary> getDictionaryList() {
        log.info("获取数据字典列表");
        String cacheKey = CacheConstants.DICT_LIST_CACHE_KEY;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<Dictionary> list = (List<Dictionary>) cached;
                return list;
            } catch (ClassCastException ignored) {}
        }

        QueryWrapper<Dictionary> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
               .orderByAsc("dict_type", "dict_sort");
        List<Dictionary> list = dictionaryMapper.selectList(wrapper);

        // 设置兼容字段
        for (Dictionary dict : list) {
            if (dict.getDictLabel() != null) {
                dict.setDictCode(dict.getDictLabel());
                dict.setDictName(dict.getDictLabel());
            }
        }
        // TTL为-1表示永不过期
        redisUtil.set(cacheKey, list);
        return list;
    }

    @Override
    public List<Dictionary> getDictionaryListByType(String type) {
        log.info("获取数据字典列表，类型：{}", type);
        String cacheKey = CacheConstants.DICT_LIST_CACHE_KEY + ":type:" + type;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<Dictionary> list = (List<Dictionary>) cached;
                return list;
            } catch (ClassCastException ignored) {}
        }

        QueryWrapper<Dictionary> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
               .eq("dict_type", type)
               .orderByAsc("dict_sort");
        List<Dictionary> list = dictionaryMapper.selectList(wrapper);

        // 设置兼容字段
        for (Dictionary dict : list) {
            if (dict.getDictLabel() != null) {
                dict.setDictCode(dict.getDictLabel());
                dict.setDictName(dict.getDictLabel());
            }
        }
        // TTL为-1表示永不过期
        redisUtil.set(cacheKey, list);
        return list;
    }

    @Override
    public boolean addDictionary(Dictionary dictionary) {
        log.info("添加数据字典，字典名称：{}", dictionary.getDictName());

        // 同步兼容字段到数据库字段
        if (dictionary.getDictLabel() == null) {
            if (dictionary.getDictName() != null) {
                dictionary.setDictLabel(dictionary.getDictName());
            } else if (dictionary.getDictCode() != null) {
                dictionary.setDictLabel(dictionary.getDictCode());
            }
        }

        dictionary.setStatus(1);
        boolean result = dictionaryMapper.insert(dictionary) > 0;
        if (result) {
            // 清除所有数据字典相关缓存
            clearDictionaryCache();
        }
        return result;
    }

    @Override
    public boolean updateDictionary(Dictionary dictionary) {
        log.info("更新数据字典，字典ID：{}", dictionary.getId());

        // 同步兼容字段到数据库字段
        if (dictionary.getDictLabel() == null) {
            if (dictionary.getDictName() != null) {
                dictionary.setDictLabel(dictionary.getDictName());
            } else if (dictionary.getDictCode() != null) {
                dictionary.setDictLabel(dictionary.getDictCode());
            }
        }

        boolean result = dictionaryMapper.updateById(dictionary) > 0;
        if (result) {
            // 清除所有数据字典相关缓存
            clearDictionaryCache();
        }
        return result;
    }

    @Override
    public boolean deleteDictionary(Long id) {
        log.info("删除数据字典，字典ID：{}", id);

        // 直接删除字典项（数据库表是扁平结构，没有父子关系）
        boolean result = dictionaryMapper.deleteById(id) > 0;
        if (result) {
            // 清除所有数据字典相关缓存
            clearDictionaryCache();
        }
        return result;
    }

    // ==================== 用户管理 ====================

    @Override
    public IPage<User> getUserList(Map<String, Object> params) {
        log.info("获取用户列表，参数：{}", params);

        // 安全地获取分页参数
        Integer page = 1;
        Integer pageSize = SystemConstants.DEFAULT_PAGE_SIZE;

        if (params.get("page") != null) {
            try {
                page = Integer.parseInt(params.get("page").toString());
            } catch (NumberFormatException e) {
                log.warn("无效的页码参数：{}", params.get("page"));
            }
        }

        if (params.get("pageSize") != null) {
            try {
                pageSize = Integer.parseInt(params.get("pageSize").toString());
            } catch (NumberFormatException e) {
                log.warn("无效的页面大小参数：{}", params.get("pageSize"));
            }
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 搜索关键词
        if (params.containsKey("searchText") && StringUtils.hasText((String) params.get("searchText"))) {
            String searchText = (String) params.get("searchText");
            String finalSearchText = searchText;
            wrapper.and(w -> w.like(User::getUsername, finalSearchText)
                            .or()
                            .like(User::getRealName, finalSearchText)
                            .or()
                            .eq(User::getPhone, finalSearchText));
        }

        // 角色类型筛选
        if (params.containsKey("roleType") && params.get("roleType") != null && StringUtils.hasText(params.get("roleType").toString())) {
            try {
                Integer roleType = Integer.parseInt(params.get("roleType").toString());
                wrapper.eq(User::getRoleType, roleType);
            } catch (NumberFormatException e) {
                log.warn("无效的角色类型参数：{}", params.get("roleType"));
            }
        }

        // 状态筛选
        if (params.containsKey("status") && params.get("status") != null && StringUtils.hasText(params.get("status").toString())) {
            try {
                Integer status = Integer.parseInt(params.get("status").toString());
                wrapper.eq(User::getStatus, status);
            } catch (NumberFormatException e) {
                log.warn("无效的状态参数：{}", params.get("status"));
            }
        }

        wrapper.orderByDesc(User::getCreateTime);

        // 构建热门分页缓存键（使用参数哈希简化层级）
        Map<String, Object> filterParams = new java.util.HashMap<>();
        if (params.containsKey("searchText") && StringUtils.hasText((String) params.get("searchText"))) {
            filterParams.put("search", params.get("searchText"));
        }
        if (params.containsKey("roleType") && params.get("roleType") != null && StringUtils.hasText(params.get("roleType").toString())) {
            filterParams.put("role", params.get("roleType"));
        }
        if (params.containsKey("status") && params.get("status") != null && StringUtils.hasText(params.get("status").toString())) {
            filterParams.put("status", params.get("status"));
        }

        String cacheKey = redisUtil.buildCacheKey(CacheConstants.USER_LIST_CACHE_PREFIX, page, pageSize, filterParams);

        if (page <= CacheConstants.USER_LIST_HOT_PAGES) {
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof IPage) {
                @SuppressWarnings("unchecked")
                IPage<User> cachedPage = (IPage<User>) cached;
                return cachedPage;
            }
        }

        Page<User> pageObject = new Page<>(page, pageSize);
        IPage<User> result = userMapper.selectPage(pageObject, wrapper);
        
        // 为每个用户的头像生成签名URL并缓存
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            result.getRecords().forEach(user -> {
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    String signedAvatarUrl = resolveAvatarUrlWithCache(user.getAvatar(), user.getId(), 
                        user.getRoleType() != null && user.getRoleType() == 2 ? "doctor" : "patient");
                    user.setAvatar(signedAvatarUrl);
                }
            });
        }
        
        if (page <= CacheConstants.USER_LIST_HOT_PAGES) {
            // TTL为-1表示永不过期
            redisUtil.set(cacheKey, result);
        }
        return result;
    }

    /**
     * 清理头像URL（移除查询参数等）
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
            }
        }
        return sanitized;
    }
    
    /**
     * 生成可直接访问的头像URL（带缓存优化）
     * 先检查Redis缓存，如果存在且未过期则更新TTL，如果过期或不存在则重新生成
     * 
     * @param rawAvatar 原始头像URL
     * @param entityId 实体ID（用户ID或医生ID）
     * @param entityType 实体类型（patient/doctor/admin）
     * @return 签名后的头像URL
     */
    private String resolveAvatarUrlWithCache(String rawAvatar, Long entityId, String entityType) {
        String sanitizedAvatar = sanitizeAvatarUrl(rawAvatar);
        if (!StringUtils.hasText(sanitizedAvatar)) {
            // 使用默认头像
            if ("doctor".equals(entityType) && entityId != null) {
                return avatarConfig.getDefaultDoctor() + "&seed=" + entityId;
            }
            if ("admin".equals(entityType) && entityId != null) {
                return avatarConfig.getDefaultAdmin() + "&seed=" + entityId;
            }
            return avatarConfig.getDefaultPatient() + (entityId != null ? "&seed=" + entityId : "");
        }
        
        // 构建缓存键
        String cacheKey = CacheConstants.CACHE_OSS_SIGNED_URL_PREFIX + sanitizedAvatar;
        
        try {
            // 先检查Redis中是否存在缓存
            Object cached = redisUtil.get(cacheKey);
            if (cached != null && cached instanceof String) {
                // 缓存存在，检查是否过期
                Long expireTime = redisUtil.getExpire(cacheKey);
                if (expireTime != null && expireTime > 0) {
                    // 缓存未过期，重置TTL（续期）
                    redisUtil.expire(cacheKey, CacheConstants.CACHE_OSS_SIGNED_URL_TTL_SECONDS, TimeUnit.SECONDS);
                    log.debug("头像签名URL缓存命中，已续期: avatar={}, expireTime={}秒", sanitizedAvatar, expireTime);
                    return (String) cached;
                } else {
                    // 缓存已过期或即将过期，删除旧缓存
                    redisUtil.delete(cacheKey);
                    log.debug("头像签名URL缓存已过期，将重新生成: avatar={}", sanitizedAvatar);
                }
            }
            
            // 缓存不存在或已过期，生成新的签名URL
            String signedUrl = ossService.generatePresignedUrl(sanitizedAvatar, avatarConfig.getTtlMinutes());
            if (StringUtils.hasText(signedUrl)) {
                // 存入缓存，TTL设置为55分钟（略小于签名URL的60分钟有效期）
                redisUtil.set(cacheKey, signedUrl, CacheConstants.CACHE_OSS_SIGNED_URL_TTL_SECONDS, TimeUnit.SECONDS);
                log.debug("生成头像签名URL并缓存: avatar={}", sanitizedAvatar);
                return signedUrl;
            } else {
                log.warn("生成头像签名URL失败，返回原始URL: avatar={}", sanitizedAvatar);
                return sanitizedAvatar;
            }
        } catch (Exception e) {
            log.warn("处理头像签名URL失败: avatar={}, error={}", sanitizedAvatar, e.getMessage());
            return sanitizedAvatar;
        }
    }

    @Override
    public boolean addUser(User user) {
        log.info("添加用户，用户名：{}", user.getUsername());

        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            log.warn("用户名已存在：{}", user.getUsername());
            return false;
        }

        // 检查手机号是否已存在
        if (StringUtils.hasText(user.getPhone())) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, user.getPhone());
            if (userMapper.selectCount(wrapper) > 0) {
                log.warn("手机号已存在：{}", user.getPhone());
                return false;
            }
        }

        // 设置默认值
        user.setStatus(1);
        if (!StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode("123456")); // 默认密码
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        boolean inserted = userMapper.insert(user) > 0;
        if (inserted) {
            try {
                // 失效存在性检查缓存键
                redisUtil.delete("hospital:common:user:exists:username:" + user.getUsername());
                if (StringUtils.hasText(user.getPhone())) {
                    redisUtil.delete("hospital:common:user:exists:phone:" + user.getPhone());
                }
                // 失效用户列表热门分页缓存
                redisUtil.deleteByPattern(CacheConstants.USER_LIST_CACHE_PATTERN);
            } catch (Exception ignored) {}
        }
        return inserted;
    }

    @Override
    public boolean updateUser(User user) {
        log.info("更新用户，用户ID：{}", user.getId());

        // 如果修改了密码，需要重新加密
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null); // 不更新密码字段
        }

        // 更新前读取旧值用于失效缓存
        User before = null;
        try { before = userMapper.selectById(user.getId()); } catch (Exception ignored) {}

        boolean updated = userMapper.updateById(user) > 0;
        if (updated) {
            try {
                if (before != null) {
                    redisUtil.delete("hospital:common:user:exists:username:" + before.getUsername());
                    if (StringUtils.hasText(before.getPhone())) {
                        redisUtil.delete("hospital:common:user:exists:phone:" + before.getPhone());
                    }
                }
                if (StringUtils.hasText(user.getUsername())) {
                    redisUtil.delete("hospital:common:user:exists:username:" + user.getUsername());
                }
                if (StringUtils.hasText(user.getPhone())) {
                    redisUtil.delete("hospital:common:user:exists:phone:" + user.getPhone());
                }
                // 失效用户列表热门分页缓存
                redisUtil.deleteByPattern(CacheConstants.USER_LIST_CACHE_PATTERN);
            } catch (Exception ignored) {}
        }
        return updated;
    }

    @Override
    public boolean deleteUser(Long id) {
        log.info("删除用户，用户ID：{}", id);

        // 检查是否有关联数据
        User user = userMapper.selectById(id);
        if (user == null) {
            log.warn("删除用户失败：用户不存在，用户ID={}", id);
            throw new RuntimeException("用户不存在");
        }

        // 检查是否有未完成的预约
        QueryWrapper<Appointment> appointmentWrapper = new QueryWrapper<>();
        appointmentWrapper.eq("user_id", id)
                .in("status", 
                    AppointmentStatus.PENDING_PAYMENT.getCode(),
                    AppointmentStatus.PENDING_VISIT.getCode(),
                    AppointmentStatus.CONFIRMED.getCode(),
                    AppointmentStatus.IN_PROGRESS.getCode());
        long unfinishedAppointmentCount = appointmentMapper.selectCount(appointmentWrapper);
        if (unfinishedAppointmentCount > 0) {
            log.warn("删除用户失败：存在未完成的预约，用户ID={}，未完成预约数={}", id, unfinishedAppointmentCount);
            throw new RuntimeException("该用户存在" + unfinishedAppointmentCount + "个未完成的预约，无法删除");
        }

        // 检查是否有接诊记录
        QueryWrapper<ConsultationRecord> consultationWrapper = new QueryWrapper<>();
        consultationWrapper.eq("patient_id", id);
        long consultationCount = consultationRecordMapper.selectCount(consultationWrapper);
        if (consultationCount > 0) {
            log.warn("删除用户失败：存在接诊记录，用户ID={}，接诊记录数={}", id, consultationCount);
            throw new RuntimeException("该用户存在" + consultationCount + "条接诊记录，无法删除");
        }

        // 检查是否是医生（如果是医生，检查是否有医生记录）
        if (user.getRoleType() != null && user.getRoleType() == 2) {
            QueryWrapper<Doctor> doctorWrapper = new QueryWrapper<>();
            doctorWrapper.eq("user_id", id);
            long doctorCount = doctorMapper.selectCount(doctorWrapper);
            if (doctorCount > 0) {
                log.warn("删除用户失败：存在医生记录，用户ID={}", id);
                throw new RuntimeException("该用户是医生，存在医生记录，无法删除。请先删除医生记录");
            }
        }

        // 删除前读取用户信息用于失效缓存
        User toDelete = null;
        try { toDelete = userMapper.selectById(id); } catch (Exception ignored) {}

        boolean deleted = userMapper.deleteById(id) > 0;
        if (deleted && toDelete != null) {
            try {
                redisUtil.delete("hospital:common:user:exists:username:" + toDelete.getUsername());
                if (StringUtils.hasText(toDelete.getPhone())) {
                    redisUtil.delete("hospital:common:user:exists:phone:" + toDelete.getPhone());
                }
                // 失效用户列表热门分页缓存
                redisUtil.deleteByPattern(CacheConstants.USER_LIST_CACHE_PATTERN);
            } catch (Exception ignored) {}
        }
        return deleted;
    }

    @Override
    public void refreshUserListCache() {
        try {
            log.info("开始刷新用户列表缓存...");
            redisUtil.deleteByPattern(CacheConstants.USER_LIST_CACHE_PATTERN);
            log.info("已刷新用户列表缓存");
        } catch (Exception e) {
            log.error("⚠️ 刷新用户列表缓存失败！这可能导致前端数据不同步", e);
            throw new RuntimeException("Redis缓存刷新失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetUserPassword(Long userId) {
        log.info("重置用户密码，用户ID：{}", userId);

        // 从配置读取默认密码
        String newPassword = userConfig.getDefaultPassword();
        String encodedPassword = passwordEncoder.encode(newPassword);

        User user = new User();
        user.setId(userId);
        user.setPassword(encodedPassword);

        boolean result = userMapper.updateById(user) > 0;

        return result ? newPassword : null;
    }

    @Override
    public boolean updateUserStatus(Long userId, Integer status) {
        log.info("更新用户状态，用户ID：{}，新状态：{}", userId, status);

        try {
            User user = new User();
            user.setId(userId);
            user.setStatus(status);

            int result = userMapper.updateById(user);
            log.info("用户状态更新结果：{}", result > 0 ? "成功" : "失败");

            if (result > 0) {
                try {
                    // 失效用户列表热门分页缓存，确保前端获取最新数据
                    redisUtil.deleteByPattern(CacheConstants.USER_LIST_CACHE_PATTERN);
                    log.info("已清除用户列表缓存，用户ID：{}", userId);
                } catch (Exception e) {
                    log.warn("清除用户列表缓存失败，用户ID：{}", userId, e);
                    // 缓存清除失败不影响主流程
                }
            }

            return result > 0;
        } catch (Exception e) {
            log.error("更新用户状态失败", e);
            return false;
        }
    }

    /**
     * 管理员强制下线指定用户：删除Redis中保存的令牌与会话键
     */
    @Override
    public boolean forceLogout(Long userId) {
        try {
            String userTokenKey = "hospital:auth:user:" + userId + ":token";
            Object tokenObj = redisUtil.get(userTokenKey);
            String token = tokenObj != null ? String.valueOf(tokenObj) : null;

            if (token != null) {
                String tokenKey = "hospital:auth:token:" + token;
                redisUtil.delete(tokenKey);
            }
            redisUtil.delete(userTokenKey);

            log.info("管理员强制下线成功：userId={}, 删除键：{} 和其令牌会话", userId, userTokenKey);
            return true;
        } catch (Exception e) {
            log.error("管理员强制下线失败：userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * 生成随机密码
     */
    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 清除所有数据字典相关缓存
     */
    private void clearDictionaryCache() {
        try {
            // 清除总列表缓存
            redisUtil.delete(CacheConstants.DICT_LIST_CACHE_KEY);
            // 清除所有按类型的缓存
            redisUtil.deleteByPattern(CacheConstants.DICT_LIST_CACHE_KEY + ":type:*");
        } catch (Exception e) {
            log.warn("清除数据字典缓存失败", e);
        }
    }
}
