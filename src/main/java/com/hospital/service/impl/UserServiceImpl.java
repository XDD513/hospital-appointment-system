package com.hospital.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hospital.common.constant.SystemSettingKeys;
import com.hospital.common.exception.BusinessException;
import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import com.hospital.dto.request.LoginRequest;
import com.hospital.dto.request.RegisterRequest;
import com.hospital.dto.request.UpdateUserInfoRequest;
import com.hospital.dto.request.UserSettingsRequest;
import com.hospital.dto.response.UserSettingsResponse;
import com.hospital.dto.response.LoginResponse;
import com.hospital.dto.response.UserInfoResponse;
import com.hospital.entity.Doctor;
import com.hospital.entity.User;
import com.hospital.mapper.AppointmentMapper;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.UserMapper;
import com.hospital.config.OssConfig;
import com.hospital.service.OssService;
import com.hospital.service.UserService;
import com.hospital.config.SystemSettingManager;
import com.hospital.util.JwtUtil;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户服务实现类
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private RedisUtil redisUtil;
    
    @Autowired
    private OssConfig ossConfig;
    
    @Autowired
    private OssService ossService;

    @Autowired
    private SystemSettingManager systemSettingManager;
    
    // 用户存在性检查短缓存TTL（秒），可配置
    @Value("${hospital.cache.ttl.exists-seconds:60}")
    private long existsTtlSeconds;

    // 缓存命中率埋点（简单计数器）
    private static final AtomicLong existsUsernameHit = new AtomicLong();
    private static final AtomicLong existsUsernameMiss = new AtomicLong();
    private static final AtomicLong existsPhoneHit = new AtomicLong();
    private static final AtomicLong existsPhoneMiss = new AtomicLong();

    // 会话过期时间（秒），可在配置文件中通过 hospital.auth.token-ttl-seconds 设置
    @Value("${hospital.auth.token-ttl-seconds:7200}")
    private long tokenTtlSeconds;

    private static final String LOGIN_ATTEMPT_KEY_PREFIX = "hospital:auth:login:attempts:user:";
    private static final String LOGIN_LOCK_KEY_PREFIX = "hospital:auth:login:lock:user:";

    /**
     * 用户登录
     */
    @Override
    public Result<LoginResponse> login(LoginRequest request) {
        // 1. 根据用户名查询用户
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            return Result.error(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        if (isLoginLocked(user.getId())) {
            long remainingMinutes = getRemainingLockMinutes(user.getId());
            String msg = remainingMinutes > 0
                    ? String.format("账户已锁定，请%d分钟后再试", remainingMinutes)
                    : "账户暂时被锁定，请稍后再试";
            return Result.error(ResultCode.USER_ACCOUNT_DISABLED.getCode(), msg);
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 记录登录失败，并检查是否达到最大失败次数
            boolean shouldLock = recordLoginFailure(user.getId(), user.getUsername());
            if (shouldLock) {
                // 达到最大失败次数，账户已被锁定
                long remainingMinutes = getRemainingLockMinutes(user.getId());
                String msg = remainingMinutes > 0
                        ? String.format("账户已被锁定，请%d分钟后再试", remainingMinutes)
                        : "账户已被锁定，请稍后再试";
                return Result.error(ResultCode.USER_ACCOUNT_DISABLED.getCode(), msg);
            } else {
                // 未达到最大失败次数，提示密码错误
                return Result.error(ResultCode.USERNAME_OR_PASSWORD_ERROR);
            }
        }

        clearLoginFailure(user.getId());

        // 3. 检查账号状态
        if (user.getStatus() == 0) {
            return Result.error(ResultCode.USER_ACCOUNT_DISABLED);
        }

        // ================== 单设备登录：使旧设备Token失效 ==================
        // 在生成新token之前，检查该用户是否已有登录会话
        // 如果存在，则删除旧token对应的会话，实现单设备登录（类似QQ/微信）
        String userTokenKey = "hospital:auth:user:" + user.getId() + ":token";
        try {
            Object oldTokenObj = redisUtil.get(userTokenKey);
            if (oldTokenObj != null) {
                String oldToken = String.valueOf(oldTokenObj);
                String oldTokenKey = "hospital:auth:token:" + oldToken;
                // 删除旧token的会话信息
                redisUtil.delete(oldTokenKey);
                log.info("用户在其他设备登录，已使旧设备token失效: userId={}, oldToken={}", 
                        user.getId(), oldToken.substring(0, Math.min(20, oldToken.length())) + "...");
            }
        } catch (Exception e) {
            // Redis操作失败不影响登录流程，仅记录日志
            log.warn("检查/删除旧登录会话失败: userId={}, error={}", user.getId(), e.getMessage());
        }

        // 4. 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleType());

        // 5. 如果是医生角色，查询doctorId
        Long doctorId = user.getRoleType() == 2 ? resolveDoctorId(user.getId()) : null;

        // 6. 构建返回结果
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roleType(user.getRoleType())
                .phone(user.getPhone())
                .doctorId(doctorId)
                .build();

        // ================== Redis 会话缓存（中文注释） ==================
        // 在登录成功后，将用户的会话信息写入 Redis，用于后续接口判断登录超时/拦截等逻辑。
        // 键设计：
        // 1) auth:token:{token} -> 会话信息（userId/username/roleType/loginAt），TTL=tokenTtlSeconds
        // 2) auth:user:{userId}:token -> 当前有效令牌（字符串），TTL=tokenTtlSeconds
        long sessionTtlSeconds = resolveSessionTimeoutSeconds();

        try {
            Map<String, Object> session = new HashMap<>();
            session.put("userId", user.getId());
            session.put("username", user.getUsername());
            session.put("roleType", user.getRoleType());
            session.put("loginAt", System.currentTimeMillis());

            String tokenKey = "hospital:auth:token:" + token;
            // 注意：userTokenKey 在上面已经定义过了

            redisUtil.set(tokenKey, session, sessionTtlSeconds, TimeUnit.SECONDS);
            // 更新用户当前有效token（会覆盖旧token，实现单设备登录）
            redisUtil.set(userTokenKey, token, sessionTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 缓存失败不影响登录流程，仅记录日志
            log.warn("写入Redis会话失败: userId={}, error={}", user.getId(), e.getMessage());
        }

        // ================== 更新登录统计信息 ==================
        // 登录次数加1
        try {
            LocalDateTime now = LocalDateTime.now();
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getId, user.getId())
                    .setSql("login_count = COALESCE(login_count, 0) + 1")
                    .set(User::getUpdateTime, now);
            userMapper.update(null, updateWrapper);
        } catch (Exception e) {
            // 更新失败不影响登录流程，仅记录日志
            log.warn("更新用户登录统计失败: userId={}, error={}", user.getId(), e.getMessage());
        }

        log.info("用户登录成功: userId={}, username={}, doctorId={}", user.getId(), user.getUsername(), doctorId);
        return Result.success("登录成功", response);
    }

    /**
     * 用户注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> register(RegisterRequest request) {
        // 1. 校验两次密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Result.error(ResultCode.BAD_REQUEST.getCode(), "两次密码输入不一致");
        }

        Integer minPasswordLength = systemSettingManager.getInteger(SystemSettingKeys.SECURITY_MIN_PASSWORD_LENGTH, 6);
        if (request.getPassword() == null || request.getPassword().length() < minPasswordLength) {
            return Result.error(ResultCode.BAD_REQUEST.getCode(),
                    String.format("密码长度至少为%d位", minPasswordLength));
        }

        // 2. 检查用户名是否已存在
        if (existsByUsername(request.getUsername())) {
            return Result.error(ResultCode.USER_ALREADY_EXISTS.getCode(), "用户名已存在");
        }

        // 3. 检查手机号是否已存在
        if (existsByPhone(request.getPhone())) {
            return Result.error(ResultCode.USER_PHONE_EXISTS);
        }

        // 4. 创建用户对象
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setIdCard(request.getIdCard());
        user.setGender(request.getGender() != null ? request.getGender() : 0);
        user.setBirthDate(request.getBirthDate());
        user.setRoleType(1); // 默认为患者角色
        user.setStatus(1); // 默认启用
        // 设置默认头像（可以根据性别设置不同默认头像）
        user.setAvatar(getDefaultAvatar(user.getGender()));

        // 5. 保存到数据库
        int result = userMapper.insert(user);
        if (result > 0) {
            log.info("用户注册成功: username={}, phone={}", user.getUsername(), user.getPhone());
            // 失效可能存在的用户名/手机号存在性缓存键
            try {
                redisUtil.delete("hospital:common:user:exists:username:" + user.getUsername());
                if (user.getPhone() != null) {
                    redisUtil.delete("hospital:common:user:exists:phone:" + user.getPhone());
                }
            } catch (Exception ignored) {}
            return Result.success("注册成功");
        } else {
            throw new BusinessException(ResultCode.DB_INSERT_ERROR);
        }
    }

    /**
     * 获取用户信息
     */
    @Override
    public Result<UserInfoResponse> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.USER_NOT_FOUND);
        }

        UserInfoResponse response = BeanUtil.copyProperties(user, UserInfoResponse.class);

        // 身份证号脱敏
        if (response.getIdCard() != null && response.getIdCard().length() > 10) {
            String idCard = response.getIdCard();
            response.setIdCard(idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4));
        }
        
        // 如果是医生角色，查询doctorId
        if (user.getRoleType() == 2) {
            QueryWrapper<Doctor> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            Doctor doctor = doctorMapper.selectOne(wrapper);
            if (doctor != null) {
                response.setDoctorId(doctor.getId());
            }
        }
        
        // 如果头像URL是OSS URL，生成签名URL
        if (response.getAvatar() != null && !response.getAvatar().isEmpty()) {
            try {
                String signedUrl = ossService.generatePresignedUrl(response.getAvatar(), 60);
                response.setAvatar(signedUrl);
            } catch (Exception e) {
                log.warn("生成头像签名URL失败: avatar={}, error={}", response.getAvatar(), e.getMessage());
                // 失败时继续使用原URL
            }
        }

        return Result.success(response);
    }

    /**
     * 检查用户名是否存在
     */
    @Override
    public boolean existsByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        String cacheKey = "hospital:common:user:exists:username:" + username;
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof Boolean) {
                boolean val = (Boolean) cached;
                existsUsernameHit.incrementAndGet();
                return val;
            }
        } catch (Exception ignored) {}

        User user = userMapper.selectByUsername(username);
        boolean exists = user != null;
        existsUsernameMiss.incrementAndGet();
        try { redisUtil.set(cacheKey, exists, existsTtlSeconds, TimeUnit.SECONDS); } catch (Exception ignored) {}
        return exists;
    }

    /**
     * 检查手机号是否存在
     */
    @Override
    public boolean existsByPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        String cacheKey = "hospital:common:user:exists:phone:" + phone;
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof Boolean) {
                boolean val = (Boolean) cached;
                existsPhoneHit.incrementAndGet();
                return val;
            }
        } catch (Exception ignored) {}

        User user = userMapper.selectByPhone(phone);
        boolean exists = user != null;
        existsPhoneMiss.incrementAndGet();
        try { redisUtil.set(cacheKey, exists, existsTtlSeconds, TimeUnit.SECONDS); } catch (Exception ignored) {}
        return exists;
    }

    /**
     * 退出登录：删除Redis中的令牌和用户会话键，并更新最后登录时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> logout(Long userId, String token) {
        try {
            String tokenKey = "hospital:auth:token:" + token;
            String userTokenKey = "hospital:auth:user:" + userId + ":token";

            // 如果用户键中保存的令牌与当前令牌一致，则一起删除
            Object savedTokenObj = redisUtil.get(userTokenKey);
            String savedToken = savedTokenObj != null ? String.valueOf(savedTokenObj) : null;
            if (savedToken != null && savedToken.equals(token)) {
                redisUtil.delete(userTokenKey);
            }
            // 删除令牌对应的会话键
            redisUtil.delete(tokenKey);

            // ================== 更新最后登录时间 ==================
            try {
                LocalDateTime now = LocalDateTime.now();
                LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(User::getId, userId)
                        .set(User::getLastLoginTime, now)
                        .set(User::getUpdateTime, now);
                userMapper.update(null, updateWrapper);
            } catch (Exception e) {
                // 更新失败不影响退出登录流程，仅记录日志
                log.warn("更新用户最后登录时间失败: userId={}, error={}", userId, e.getMessage());
            }

            log.info("用户退出登录成功：userId={}, tokenKey={}, userTokenKey={}", userId, tokenKey, userTokenKey);
            return Result.success(true);
        } catch (Exception e) {
            log.warn("退出登录时删除Redis会话失败：userId={}, error={}", userId, e.getMessage());
            return Result.error("退出登录失败");
        }
    }

    /**
     * 优先从Redis缓存中解析医生ID，缓存未命中时回源数据库
     */
    private Long resolveDoctorId(Long userId) {
        String cacheKey = "hospital:doctor:profile:userId:" + userId;

        // 1. 优先从缓存获取
        try {
            Object cachedDoctor = redisUtil.get(cacheKey);
            if (cachedDoctor != null) {
                if (cachedDoctor instanceof Doctor) {
                    return ((Doctor) cachedDoctor).getId();
                }
                if (cachedDoctor instanceof Map) {
                    Object idObj = ((Map<?, ?>) cachedDoctor).get("id");
                    if (idObj != null) {
                        return Long.valueOf(String.valueOf(idObj));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从缓存获取医生信息失败: userId={}, error={}", userId, e.getMessage());
        }

        // 2. 回源数据库
        try {
            Doctor doctor = doctorMapper.selectByUserId(userId);
            if (doctor != null) {
                // 缓存回填，减少后续查询压力
                try {
                    redisUtil.set(cacheKey, doctor, 10, TimeUnit.MINUTES);
                } catch (Exception e) {
                    log.warn("缓存医生信息失败: userId={}, error={}", userId, e.getMessage());
                }
                return doctor.getId();
            }
        } catch (Exception e) {
            log.warn("数据库查询医生信息失败: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    private boolean isLoginLocked(Long userId) {
        if (!Boolean.TRUE.equals(systemSettingManager.getBoolean(SystemSettingKeys.SECURITY_LOGIN_LOCK_ENABLED, Boolean.FALSE))) {
            return false;
        }
        String lockKey = LOGIN_LOCK_KEY_PREFIX + userId;
        try {
            Boolean locked = redisUtil.hasKey(lockKey);
            return Boolean.TRUE.equals(locked);
        } catch (Exception e) {
            log.warn("判断用户是否被锁定失败：userId={}, error={}", userId, e.getMessage());
        }
        return false;
    }

    private long getRemainingLockMinutes(Long userId) {
        String lockKey = LOGIN_LOCK_KEY_PREFIX + userId;
        try {
            Long expireSeconds = redisUtil.getExpire(lockKey);
            if (expireSeconds != null && expireSeconds > 0) {
                return (expireSeconds + 59) / 60;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    /**
     * 记录登录失败次数
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @return 是否达到最大失败次数（是否需要锁定账户）
     */
    private boolean recordLoginFailure(Long userId, String username) {
        if (!Boolean.TRUE.equals(systemSettingManager.getBoolean(SystemSettingKeys.SECURITY_LOGIN_LOCK_ENABLED, Boolean.FALSE))) {
            return false;
        }
        int maxAttempts = systemSettingManager.getInteger(SystemSettingKeys.SECURITY_MAX_LOGIN_ATTEMPTS, 5);
        int lockDurationMinutes = systemSettingManager.getInteger(SystemSettingKeys.SECURITY_LOCK_DURATION, 15);
        String attemptKey = LOGIN_ATTEMPT_KEY_PREFIX + userId;
        Long attempts = null;
        try {
            // 增加失败次数
            attempts = redisUtil.increment(attemptKey, 1);
            redisUtil.expire(attemptKey, lockDurationMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("记录登录失败次数异常：userId={}, error={}", userId, e.getMessage());
            return false;
        }
        
        // 检查是否达到最大失败次数
        if (attempts != null && attempts >= maxAttempts) {
            // 达到最大失败次数，锁定账户
            String lockKey = LOGIN_LOCK_KEY_PREFIX + userId;
            try {
                // 删除失败次数记录
                redisUtil.delete(attemptKey);
                // 设置锁定标记
                String lockValue = (username != null ? username : "user-" + userId) + ":LOCKED";
                redisUtil.set(lockKey, lockValue, lockDurationMinutes, TimeUnit.MINUTES);
                log.warn("用户因多次登录失败被锁定：userId={}, attempts={}, lockDuration={}分钟", userId, attempts, lockDurationMinutes);
                return true; // 返回true表示已达到最大失败次数，需要锁定
            } catch (Exception e) {
                log.warn("设置登录锁失败：userId={}, error={}", userId, e.getMessage());
                return false;
            }
        }
        
        return false; // 未达到最大失败次数，返回false
    }

    private void clearLoginFailure(Long userId) {
        String attemptKey = LOGIN_ATTEMPT_KEY_PREFIX + userId;
        try {
            redisUtil.delete(attemptKey);
        } catch (Exception ignored) {}
    }

    private long resolveSessionTimeoutSeconds() {
        Integer sessionMinutes = systemSettingManager.getInteger(SystemSettingKeys.SECURITY_SESSION_TIMEOUT, (int) (tokenTtlSeconds / 60));
        if (sessionMinutes == null || sessionMinutes <= 0) {
            return tokenTtlSeconds;
        }
        return sessionMinutes * 60L;
    }

    /**
     * 更新用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateUserInfo(Long userId, UpdateUserInfoRequest request) {
        // 1. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.USER_NOT_FOUND);
        }

        // 2. 更新用户信息（只更新非空字段）
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getPhone() != null) {
            // 如果手机号改变，需要检查是否已存在
            if (!user.getPhone().equals(request.getPhone()) && existsByPhone(request.getPhone())) {
                return Result.error(ResultCode.USER_PHONE_EXISTS);
            }
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        if (request.getIdCard() != null) {
            user.setIdCard(request.getIdCard());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getAvatar() != null) {
            // 清理头像URL，移除OSS签名参数，只保存原始URL
            String sanitizedAvatar = sanitizeAvatarUrl(request.getAvatar());
            user.setAvatar(sanitizedAvatar);
        }

        // 3. 保存到数据库
        int result = userMapper.updateById(user);
        if (result > 0) {
            log.info("用户信息更新成功: userId={}", userId);
            // 清除相关缓存
            try {
                if (request.getPhone() != null && !user.getPhone().equals(request.getPhone())) {
                    redisUtil.delete("hospital:common:user:exists:phone:" + user.getPhone());
                    redisUtil.delete("hospital:common:user:exists:phone:" + request.getPhone());
                }
            } catch (Exception ignored) {}
            return Result.success("更新成功");
        } else {
            throw new BusinessException(ResultCode.DB_UPDATE_ERROR);
        }
    }

    /**
     * 修改密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> changePassword(Long userId, com.hospital.dto.request.ChangePasswordRequest request) {
        try {
            // 1. 查询用户
            User user = userMapper.selectById(userId);
            if (user == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }

            // 2. 检查新密码是否与原密码相同
            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "新密码不能与原密码相同");
            }

            // 3. 更新密码
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            int result = userMapper.updateById(user);
            
            if (result > 0) {
                log.info("用户密码修改成功: userId={}", userId);
                // 清除用户相关的token缓存，强制重新登录
                try {
                    redisUtil.deleteByPattern("hospital:auth:user:" + userId + ":token");
                    redisUtil.deleteByPattern("hospital:auth:token:*");
                } catch (Exception ignored) {}
                return Result.success("密码修改成功，请重新登录");
            } else {
                throw new BusinessException(ResultCode.DB_UPDATE_ERROR);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改密码失败: userId={}", userId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "修改密码失败");
        }
    }

    /**
     * 获取用户设置
     */
    @Override
    public Result<UserSettingsResponse> getUserSettings(Long userId) {
        try {
            String cacheKey = "hospital:user:settings:userId:" + userId;
            Object cached = redisUtil.get(cacheKey);
            
            if (cached instanceof UserSettingsResponse) {
                return Result.success((UserSettingsResponse) cached);
            }
            
            // 如果缓存中没有，返回默认设置
            UserSettingsResponse defaultSettings = new UserSettingsResponse();
            // 缓存默认设置（1小时）
            redisUtil.set(cacheKey, defaultSettings, 1, TimeUnit.HOURS);
            return Result.success(defaultSettings);
        } catch (Exception e) {
            log.error("获取用户设置失败: userId={}", userId, e);
            // 返回默认设置
            return Result.success(new UserSettingsResponse());
        }
    }

    /**
     * 更新用户设置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateUserSettings(Long userId, UserSettingsRequest request) {
        try {
            String cacheKey = "hospital:user:settings:userId:" + userId;
            
            // 获取现有设置或创建新设置
            UserSettingsResponse settings = new UserSettingsResponse();
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof UserSettingsResponse) {
                settings = (UserSettingsResponse) cached;
            }
            
            // 更新设置（只更新非空字段）
            if (request.getNotification() != null) {
                settings.setNotification(request.getNotification());
            }
            if (request.getSmsReminder() != null) {
                settings.setSmsReminder(request.getSmsReminder());
            }
            if (request.getAppointmentReminder() != null) {
                settings.setAppointmentReminder(request.getAppointmentReminder());
            }
            if (request.getReviewNotification() != null) {
                settings.setReviewNotification(request.getReviewNotification());
            }
            if (request.getOperationReminder() != null) {
                settings.setOperationReminder(request.getOperationReminder());
            }
            
            // 保存到Redis（永不过期，用户设置应该持久化）
            redisUtil.set(cacheKey, settings);
            
            log.info("用户设置更新成功: userId={}", userId);
            return Result.success("设置保存成功");
        } catch (Exception e) {
            log.error("更新用户设置失败: userId={}", userId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "更新设置失败");
        }
    }

    /**
     * 获取患者预约统计
     */
    @Override
    public Result<com.hospital.dto.response.PatientAppointmentStatsResponse> getPatientAppointmentStats(Long patientId) {
        try {
            String cacheKey = "hospital:patient:appointment:stats:patient:" + patientId;
            Object cached = redisUtil.get(cacheKey);
            
            if (cached instanceof com.hospital.dto.response.PatientAppointmentStatsResponse) {
                return Result.success((com.hospital.dto.response.PatientAppointmentStatsResponse) cached);
            }
            
            com.hospital.dto.response.PatientAppointmentStatsResponse stats = new com.hospital.dto.response.PatientAppointmentStatsResponse();
            stats.setTotalAppointments(appointmentMapper.countByPatientId(patientId));
            stats.setPendingAppointments(appointmentMapper.countPendingByPatientId(patientId));
            
            // 缓存5分钟
            redisUtil.set(cacheKey, stats, 5, TimeUnit.MINUTES);
            
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取患者预约统计失败: patientId={}", patientId, e);
            return Result.success(new com.hospital.dto.response.PatientAppointmentStatsResponse());
        }
    }

    /**
     * 获取默认头像URL
     * 
     * @param gender 性别（0-未知 1-男 2-女）
     * @return 默认头像URL
     */
    private String getDefaultAvatar(Integer gender) {
        // 可以根据性别返回不同的默认头像
        // 这里使用一个通用的默认头像，实际项目中可以上传默认头像到OSS
        String baseUrl = ossConfig.getUrlProtocol() + "://" + 
                         ossConfig.getBucketName() + "." + 
                         ossConfig.getEndpoint() + "/" + 
                         ossConfig.getAvatarPath();
        if (gender == null || gender == 0) {
            return baseUrl + "default-avatar.png";
        } else if (gender == 1) {
            return baseUrl + "default-male.png";
        } else {
            return baseUrl + "default-female.png";
        }
    }

    /**
     * 清理头像URL（移除查询参数等）
     * 移除OSS签名参数，只保留原始URL
     */
    private String sanitizeAvatarUrl(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return null;
        }
        String sanitized = avatarUrl.trim();
        // 检查是否包含OSS签名参数
        boolean containsSignatureParam = sanitized.contains("Signature=") || sanitized.contains("OSSAccessKeyId=")
                || sanitized.contains("Expires=");
        if (containsSignatureParam) {
            // 移除?号及其后面的所有参数
            int questionIndex = sanitized.indexOf('?');
            if (questionIndex > 0) {
                sanitized = sanitized.substring(0, questionIndex);
            } else {
                // 如果没有?号，检查是否有&号（可能是URL编码的情况）
                int ampIndex = sanitized.indexOf('&');
                if (ampIndex > 0) {
                    sanitized = sanitized.substring(0, ampIndex);
                }
            }
        }
        return sanitized;
    }
}


