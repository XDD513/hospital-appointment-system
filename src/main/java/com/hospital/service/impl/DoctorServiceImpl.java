package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospital.common.exception.BusinessException;
import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import com.hospital.entity.Doctor;
import com.hospital.entity.Department;
import com.hospital.entity.User;
import com.hospital.common.constant.AppointmentStatus;
import com.hospital.entity.Appointment;
import com.hospital.mapper.AppointmentMapper;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.UserMapper;
import com.hospital.mapper.DepartmentMapper;
import com.hospital.service.DoctorService;
import com.hospital.service.OssService;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 医生服务实现类
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
import com.hospital.common.constant.CacheConstants;
import com.hospital.common.constant.DefaultConstants;
import com.hospital.config.AvatarConfig;
import com.hospital.config.UserConfig;

@Slf4j
@Service
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private OssService ossService;

    @Autowired
    private UserConfig userConfig;

    @Autowired
    private AvatarConfig avatarConfig;

    /**
     * 查询所有医生列表
     */
    @Override
    public Result<List<Doctor>> getDoctorList() {
        try {
            String cacheKey = CacheConstants.DOCTOR_LIST_CACHE_KEY;
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Doctor> list = (List<Doctor>) cached;
                    return Result.success(buildDoctorResponseList(list));
                } catch (ClassCastException ignored) {}
            }

            List<Doctor> doctors = doctorMapper.selectList(null);

            // 手动关联分类名称和用户信息
            for (Doctor doctor : doctors) {
                // 关联分类名称
                if (doctor.getCategoryId() != null) {
                    try {
                        com.hospital.entity.Department department = departmentMapper.selectById(doctor.getCategoryId());
                        if (department != null) {
                            doctor.setCategoryName(department.getCategoryName());
                            doctor.setDeptName(department.getCategoryName()); // 兼容旧字段
                        }
                    } catch (Exception e) {
                        log.warn("获取分类名称失败: categoryId={}", doctor.getCategoryId(), e);
                    }
                }
                // 设置兼容字段
                if (doctor.getCategoryId() != null && doctor.getDeptId() == null) {
                    doctor.setDeptId(doctor.getCategoryId());
                }

                // 关联用户信息
                if (doctor.getUserId() != null) {
                    try {
                        User user = userMapper.selectById(doctor.getUserId());
                        if (user != null) {
                            doctor.setGender(user.getGender());
                            doctor.setBirthday(user.getBirthDate());
                            if (StringUtils.hasText(user.getAvatar())) {
                                doctor.setAvatar(user.getAvatar());
                            }
                            // 补充缺失的基本信息
                            if (doctor.getDoctorName() == null || doctor.getDoctorName().isEmpty()) {
                                doctor.setDoctorName(user.getRealName());
                            }
                            if (doctor.getPhone() == null || doctor.getPhone().isEmpty()) {
                                doctor.setPhone(user.getPhone());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("获取用户信息失败: userId={}", doctor.getUserId(), e);
                    }
                }
            }

            // 医生列表缓存（永久）
            redisUtil.set(cacheKey, doctors);
            return Result.success(buildDoctorResponseList(doctors));
        } catch (Exception e) {
            log.error("查询医生列表失败", e);
            return Result.error("查询医生列表失败");
        }
    }

    /**
     * 查询在职医生列表
     */
    @Override
    public Result<List<Doctor>> getEnabledDoctorList() {
        try {
            String cacheKey = CacheConstants.DOCTOR_LIST_ENABLED_CACHE_KEY;
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Doctor> list = (List<Doctor>) cached;
                    return Result.success(buildDoctorResponseList(list));
                } catch (ClassCastException ignored) {}
            }

            List<Doctor> doctors = doctorMapper.selectEnabledList();

            // 手动关联分类名称和用户信息
            for (Doctor doctor : doctors) {
                // 关联分类名称
                if (doctor.getCategoryId() != null) {
                    try {
                        com.hospital.entity.Department department = departmentMapper.selectById(doctor.getCategoryId());
                        if (department != null) {
                            doctor.setCategoryName(department.getCategoryName());
                            doctor.setDeptName(department.getCategoryName()); // 兼容旧字段
                        }
                    } catch (Exception e) {
                        log.warn("获取分类名称失败: categoryId={}", doctor.getCategoryId(), e);
                    }
                }
                // 设置兼容字段
                if (doctor.getCategoryId() != null && doctor.getDeptId() == null) {
                    doctor.setDeptId(doctor.getCategoryId());
                }

                // 关联用户信息
                if (doctor.getUserId() != null) {
                    try {
                        User user = userMapper.selectById(doctor.getUserId());
                        if (user != null) {
                            doctor.setGender(user.getGender());
                            doctor.setBirthday(user.getBirthDate());
                            if (StringUtils.hasText(user.getAvatar())) {
                                doctor.setAvatar(user.getAvatar());
                            }
                            // 补充缺失的基本信息
                            if (doctor.getDoctorName() == null || doctor.getDoctorName().isEmpty()) {
                                doctor.setDoctorName(user.getRealName());
                            }
                            if (doctor.getPhone() == null || doctor.getPhone().isEmpty()) {
                                doctor.setPhone(user.getPhone());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("获取用户信息失败: userId={}", doctor.getUserId(), e);
                    }
                }
            }

            // 在职医生列表缓存（永久）
            redisUtil.set(cacheKey, doctors);
            return Result.success(buildDoctorResponseList(doctors));
        } catch (Exception e) {
            log.error("查询医生列表失败", e);
            return Result.error("查询医生列表失败");
        }
    }

    /**
     * 根据科室ID查询医生列表
     */
    @Override
    public Result<List<Doctor>> getDoctorListByDeptId(Long deptId) {
        try {
            String cacheKey = CacheConstants.DOCTOR_LIST_BY_DEPT_CACHE_PREFIX + deptId;
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Doctor> list = (List<Doctor>) cached;
                    return Result.success(buildDoctorResponseList(list));
                } catch (ClassCastException ignored) {}
            }

            List<Doctor> doctors = doctorMapper.selectByDeptId(deptId);

            // 手动关联分类名称和用户信息
            for (Doctor doctor : doctors) {
                // 关联分类名称
                if (doctor.getCategoryId() != null) {
                    try {
                        com.hospital.entity.Department department = departmentMapper.selectById(doctor.getCategoryId());
                        if (department != null) {
                            doctor.setCategoryName(department.getCategoryName());
                            doctor.setDeptName(department.getCategoryName()); // 兼容旧字段
                        }
                    } catch (Exception e) {
                        log.warn("获取分类名称失败: categoryId={}", doctor.getCategoryId(), e);
                    }
                }
                // 设置兼容字段
                if (doctor.getCategoryId() != null && doctor.getDeptId() == null) {
                    doctor.setDeptId(doctor.getCategoryId());
                }

                // 关联用户信息
                if (doctor.getUserId() != null) {
                    try {
                        User user = userMapper.selectById(doctor.getUserId());
                        if (user != null) {
                            doctor.setGender(user.getGender());
                            doctor.setBirthday(user.getBirthDate());
                            if (StringUtils.hasText(user.getAvatar())) {
                                doctor.setAvatar(user.getAvatar());
                            }
                            // 补充缺失的基本信息
                            if (doctor.getDoctorName() == null || doctor.getDoctorName().isEmpty()) {
                                doctor.setDoctorName(user.getRealName());
                            }
                            if (doctor.getPhone() == null || doctor.getPhone().isEmpty()) {
                                doctor.setPhone(user.getPhone());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("获取用户信息失败: userId={}", doctor.getUserId(), e);
                    }
                }
            }

            // 科室医生列表缓存（永久）
            redisUtil.set(cacheKey, doctors);
            return Result.success(buildDoctorResponseList(doctors));
        } catch (Exception e) {
            log.error("查询科室医生列表失败: deptId={}", deptId, e);
            return Result.error("查询医生列表失败");
        }
    }

    /**
     * 根据ID查询医生详情
     */
    @Override
    public Result<Doctor> getDoctorById(Long id) {
        String cacheKey = CacheConstants.CACHE_DOCTOR_PREFIX + id;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof Doctor) {
            return Result.success(cloneDoctorWithSignedAvatar((Doctor) cached));
        }

        Doctor doctor = doctorMapper.selectById(id);
        if (doctor == null) {
            return Result.error(ResultCode.DOCTOR_NOT_FOUND);
        }

        // 关联查询分类名称
        if (doctor.getCategoryId() != null) {
            try {
                com.hospital.entity.Department department = departmentMapper.selectById(doctor.getCategoryId());
                if (department != null) {
                    doctor.setCategoryName(department.getCategoryName());
                    doctor.setDeptName(department.getCategoryName()); // 兼容旧字段
                }
            } catch (Exception e) {
                log.warn("获取分类名称失败: categoryId={}", doctor.getCategoryId(), e);
            }
        }
        // 设置兼容字段
        if (doctor.getCategoryId() != null && doctor.getDeptId() == null) {
            doctor.setDeptId(doctor.getCategoryId());
        }

        // 关联查询用户信息（性别、出生日期等）
        if (doctor.getUserId() != null) {
            try {
                User user = userMapper.selectById(doctor.getUserId());
                if (user != null) {
                    doctor.setGender(user.getGender());
                    doctor.setBirthday(user.getBirthDate());
                    doctor.setAvatar(user.getAvatar());
                    // 如果医生姓名为空，使用用户的真实姓名
                    if (doctor.getDoctorName() == null || doctor.getDoctorName().isEmpty()) {
                        doctor.setDoctorName(user.getRealName());
                    }
                    // 如果手机号为空，使用用户的手机号
                    if (doctor.getPhone() == null || doctor.getPhone().isEmpty()) {
                        doctor.setPhone(user.getPhone());
                    }
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败: userId={}", doctor.getUserId(), e);
            }
        }

        // 医生详情缓存（永久）
        redisUtil.set(cacheKey, doctor);
        return Result.success(cloneDoctorWithSignedAvatar(doctor));
    }

    /**
     * 添加医生
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addDoctor(Doctor doctor) {
        try {
            // 1. 先创建用户账号
            User user = new User();
            String doctorName = doctor.getDoctorName();
            if (!StringUtils.hasText(doctorName)) {
                throw new BusinessException(ResultCode.DOCTOR_NOT_FOUND, "医生姓名不能为空");
            }

            user.setUsername(doctorName + "_" + System.currentTimeMillis()); // 生成唯一用户名
            user.setPassword(passwordEncoder.encode(userConfig.getDefaultPassword())); // 从配置读取默认密码
            user.setRealName(doctorName);
            user.setPhone(doctor.getPhone()); // 手机号
            user.setGender(doctor.getGender()); // 性别
            user.setBirthDate(doctor.getBirthday()); // 出生日期
            user.setRoleType(2); // 医生角色
            user.setStatus(1); // 启用状态

            int userResult = userMapper.insert(user);
            if (userResult <= 0) {
                throw new BusinessException(ResultCode.DB_INSERT_ERROR);
            }

            // 2. 设置医生信息
            doctor.setUserId(user.getId());
            doctor.setDoctorName(doctor.getDoctorName());

            // 2.5. 同步兼容字段到数据库字段
            if (doctor.getDeptId() != null && doctor.getCategoryId() == null) {
                doctor.setCategoryId(doctor.getDeptId());
                log.info("同步字段: deptId={} -> categoryId={}", doctor.getDeptId(), doctor.getCategoryId());
            }

            // 验证 categoryId 是否有值
            if (doctor.getCategoryId() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "科室ID不能为空");
            }

            // 3. 设置默认值
            if (doctor.getStatus() == null) {
                doctor.setStatus(1); // 默认在职
            }
            if (doctor.getRating() == null) {
                doctor.setRating(new java.math.BigDecimal("0.00"));
            }
            if (doctor.getConsultationCount() == null) {
                doctor.setConsultationCount(0);
            }

            // 4. 保存医生到数据库
            int result = doctorMapper.insert(doctor);
            if (result > 0) {
                log.info("添加医生成功: doctorId={}, userId={}, deptId={}, doctorName={}",
                        doctor.getId(), doctor.getUserId(), doctor.getDeptId(), doctor.getDoctorName());

                // 验证ID生成是否正确
                if (doctor.getId() == null) {
                    log.error("医生ID生成失败，ID为null");
                    throw new BusinessException(ResultCode.DB_INSERT_ERROR, "医生ID生成失败");
                }

                // 验证关联ID是否正确
                if (!doctor.getUserId().equals(user.getId())) {
                    log.error("医生userId与用户ID不匹配: doctorUserId={}, userActualId={}",
                            doctor.getUserId(), user.getId());
                }

                // 主动更新缓存
                refreshAllDoctorCaches();
                return Result.success("添加成功");
            } else {
                throw new BusinessException(ResultCode.DB_INSERT_ERROR);
            }
        } catch (Exception e) {
            log.error("添加医生失败", e);
            throw new BusinessException(ResultCode.DB_INSERT_ERROR);
        }
    }

    /**
     * 更新医生信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateDoctor(Doctor doctor) {
        // 1. 检查医生是否存在
        Doctor existDoctor = doctorMapper.selectById(doctor.getId());
        if (existDoctor == null) {
            return Result.error(ResultCode.DOCTOR_NOT_FOUND);
        }

        // 1.5. 同步兼容字段到数据库字段
        if (doctor.getDeptId() != null && doctor.getCategoryId() == null) {
            doctor.setCategoryId(doctor.getDeptId());
            log.info("更新医生时同步字段: deptId={} -> categoryId={}", doctor.getDeptId(), doctor.getCategoryId());
        }

        // 2. 更新数据库
        int result = doctorMapper.updateById(doctor);
        if (result > 0) {
            log.info("更新医生成功: id={}", doctor.getId());
            // 主动更新缓存
            refreshAllDoctorCaches();
            return Result.success("更新成功");
        } else {
            throw new BusinessException(ResultCode.DB_UPDATE_ERROR);
        }
    }

    /**
     * 删除医生
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteDoctor(Long id) {
        try {
            Doctor doctor;
            Long userId = null;

            // 1. 首先尝试根据医生ID查找医生记录
            doctor = doctorMapper.selectById(id);
            if (doctor != null) {
                userId = doctor.getUserId();
                log.info("根据医生ID找到医生记录: doctorId={}, userId={}", id, userId);
            } else {
                // 2. 如果没找到，可能是传递的是用户ID，尝试根据用户ID查找医生
                log.warn("根据医生ID未找到记录，尝试根据用户ID查找: id={}", id);
                QueryWrapper<Doctor> wrapper = new QueryWrapper<>();
                wrapper.eq("user_id", id);
                doctor = doctorMapper.selectOne(wrapper);
                if (doctor != null) {
                    userId = id; // 传递的ID就是用户ID
                    log.info("根据用户ID找到医生记录: doctorId={}, userId={}", doctor.getId(), userId);
                } else {
                    // 3. 如果还是没找到，尝试根据ID的字符串匹配查找（处理ID不匹配的情况）
                    log.warn("根据用户ID也未找到记录，尝试模糊匹配: id={}", id);
                    List<Doctor> allDoctors = doctorMapper.selectList(null);
                    for (Doctor d : allDoctors) {
                        if (d.getId().toString().contains(id.toString().substring(0, Math.min(10, id.toString().length())))) {
                            doctor = d;
                            userId = d.getUserId();
                            log.info("通过模糊匹配找到医生记录: doctorId={}, userId={}", doctor.getId(), userId);
                            break;
                        }
                    }
                }
            }

            if (doctor == null) {
                return Result.error(ResultCode.DOCTOR_NOT_FOUND);
            }

            // 4. 检查是否有未完成的预约
            QueryWrapper<Appointment> appointmentWrapper = new QueryWrapper<>();
            appointmentWrapper.eq("doctor_id", doctor.getId())
                    .in("status", 
                        AppointmentStatus.PENDING_PAYMENT.getCode(),
                        AppointmentStatus.PENDING_VISIT.getCode(),
                        AppointmentStatus.CONFIRMED.getCode(),
                        AppointmentStatus.IN_PROGRESS.getCode());
            long unfinishedAppointmentCount = appointmentMapper.selectCount(appointmentWrapper);
            if (unfinishedAppointmentCount > 0) {
                log.warn("删除医生失败：存在未完成的预约，医生ID={}，未完成预约数={}", doctor.getId(), unfinishedAppointmentCount);
                return Result.error(ResultCode.DOCTOR_HAS_UNFINISHED_APPOINTMENTS);
            }

            // 5. 先删除医生记录
            int doctorResult = doctorMapper.deleteById(doctor.getId());
            if (doctorResult <= 0) {
                throw new BusinessException(ResultCode.DB_DELETE_ERROR);
            }

            // 6. 再删除用户记录
            if (userId != null) {
                int userResult = userMapper.deleteById(userId);
                if (userResult <= 0) {
                    log.warn("删除用户记录失败: userId={}", userId);
                    // 注意：这里不抛出异常，因为医生记录已经删除成功
                    // 可以考虑记录日志或发送告警
                } else {
                    log.info("删除用户记录成功: userId={}", userId);
                }
            }

            log.info("删除医生成功: doctorId={}, userId={}", doctor.getId(), userId);
            // 主动更新缓存
            refreshAllDoctorCaches();
            return Result.success("删除成功");

        } catch (Exception e) {
            log.error("删除医生失败: id={}", id, e);
            throw new BusinessException(ResultCode.DB_DELETE_ERROR);
        }
    }

    /**
     * 根据用户ID获取医生个人信息
     */
    @Override
    public Result<Doctor> getDoctorProfileByUserId(Long userId) {
        try {
            log.info("根据用户ID获取医生信息: userId={}", userId);

            // 先从缓存获取
            String cacheKey = "hospital:doctor:profile:userId:" + userId;
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof Doctor) {
                log.info("从缓存获取医生信息成功");
                return Result.success((Doctor) cached);
            }

            // 从数据库查询
            QueryWrapper<Doctor> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            Doctor doctor = doctorMapper.selectOne(wrapper);

            if (doctor == null) {
                log.warn("医生信息不存在: userId={}", userId);
                return Result.error("医生信息不存在");
            }

            // 补充用户与科室信息
            enrichDoctorInfo(doctor);

            // 查询科室名称
            if (doctor.getDeptId() != null) {
                Department dept = departmentMapper.selectById(doctor.getDeptId());
                if (dept != null) {
                    doctor.setDeptName(dept.getDeptName());
                }
            }

            // 缓存医生信息（永久）
            redisUtil.set(cacheKey, doctor);

            log.info("获取医生信息成功: doctorId={}", doctor.getId());
            return Result.success(doctor);

        } catch (Exception e) {
            log.error("获取医生信息失败: userId={}", userId, e);
            return Result.error("获取医生信息失败");
        }
    }

    /**
     * 更新医生个人信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateDoctorProfile(Doctor doctor) {
        try {
            log.info("更新医生个人信息: userId={}", doctor.getUserId());

            // 查询当前医生信息
            QueryWrapper<Doctor> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", doctor.getUserId());
            Doctor existingDoctor = doctorMapper.selectOne(wrapper);

            if (existingDoctor == null) {
                log.warn("医生信息不存在: userId={}", doctor.getUserId());
                return Result.error("医生信息不存在");
            }

            // 只允许更新部分字段（不能修改科室、状态等敏感字段）
            existingDoctor.setDoctorName(doctor.getDoctorName());
            existingDoctor.setPhone(doctor.getPhone());
            existingDoctor.setGender(doctor.getGender());
            existingDoctor.setTitle(doctor.getTitle());
            existingDoctor.setSpecialty(doctor.getSpecialty());
            existingDoctor.setIntroduction(doctor.getIntroduction());
            existingDoctor.setYearsOfExperience(doctor.getYearsOfExperience());

            // 更新数据库
            int result = doctorMapper.updateById(existingDoctor);
            if (result <= 0) {
                log.error("更新医生信息失败: doctorId={}", existingDoctor.getId());
                return Result.error("更新医生信息失败");
            }

            // 同步更新用户表的真实姓名、手机号、头像
            if (existingDoctor.getUserId() != null) {
                User user = userMapper.selectById(existingDoctor.getUserId());
                if (user != null) {
                    if (StringUtils.hasText(doctor.getDoctorName())) {
                        user.setRealName(doctor.getDoctorName());
                    }
                    if (doctor.getPhone() != null) {
                        user.setPhone(doctor.getPhone());
                    }
                    if (StringUtils.hasText(doctor.getAvatar())) {
                        user.setAvatar(doctor.getAvatar());
                    }
                    userMapper.updateById(user);
                }
            }

            // 主动更新缓存
            refreshAllDoctorCaches();

            log.info("更新医生信息成功: doctorId={}", existingDoctor.getId());
            return Result.success("更新成功");

        } catch (Exception e) {
            log.error("更新医生信息失败: userId={}", doctor.getUserId(), e);
            throw new BusinessException(ResultCode.DB_UPDATE_ERROR);
        }
    }

    /**
     * 更新医生状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateDoctorStatus(Long id, Integer status) {
        log.info("更新医生状态: id={}, status={}", id, status);

        // 1. 检查医生是否存在
        Doctor existDoctor = doctorMapper.selectById(id);
        if (existDoctor == null) {
            return Result.error(ResultCode.DOCTOR_NOT_FOUND);
        }

        // 2. 验证状态值
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "状态参数无效");
        }

        // 3. 更新状态
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setStatus(status);

        int result = doctorMapper.updateById(doctor);
        if (result > 0) {
            log.info("更新医生状态成功: id={}, name={}, status={}",
                    id, existDoctor.getDoctorName(), status == 1 ? "在职" : "离职");
            // 主动更新缓存
            refreshAllDoctorCaches();
            return Result.success(status == 1 ? "启用成功" : "禁用成功");
        } else {
            throw new BusinessException(ResultCode.DB_UPDATE_ERROR);
        }
    }

    /**
     * 刷新所有医生缓存
     */
    @Override
    public void refreshAllDoctorCaches() {
        try {
            log.info("开始刷新医生缓存...");

            // 1. 刷新所有医生列表缓存
            List<Doctor> allDoctors = doctorMapper.selectList(null);
            for (Doctor doctor : allDoctors) {
                enrichDoctorInfo(doctor);
            }
            redisUtil.set(CacheConstants.DOCTOR_LIST_CACHE_KEY, allDoctors);
            log.info("已刷新缓存: hospital:common:doctor:list, 共{}条记录", allDoctors.size());

            // 2. 刷新在职医生列表缓存
            List<Doctor> enabledDoctors = doctorMapper.selectEnabledList();
            for (Doctor doctor : enabledDoctors) {
                enrichDoctorInfo(doctor);
            }
            redisUtil.set(CacheConstants.DOCTOR_LIST_ENABLED_CACHE_KEY, enabledDoctors);
            log.info("已刷新缓存: hospital:common:doctor:list:enabled, 共{}条记录", enabledDoctors.size());

            // 3. 刷新按科室的医生列表缓存
            redisUtil.deleteByPattern(CacheConstants.DOCTOR_LIST_BY_DEPT_CACHE_PREFIX + "*");
            log.info("已删除缓存: hospital:common:doctor:list:dept:*");

            // 4. 刷新所有医生详情缓存
            for (Doctor doctor : allDoctors) {
                redisUtil.set(CacheConstants.CACHE_DOCTOR_PREFIX + doctor.getId(), doctor);
                if (doctor.getUserId() != null) {
                    redisUtil.set("hospital:doctor:profile:userId:" + doctor.getUserId(), doctor);
                }
            }
            log.info("已刷新{}个医生详情缓存", allDoctors.size());

            log.info("医生缓存刷新成功！");
        } catch (Exception e) {
            log.error("⚠️ 刷新医生缓存失败！这可能导致前端数据不同步", e);
            // 重新抛出异常，让调用方知道缓存刷新失败
            throw new RuntimeException("Redis缓存刷新失败", e);
        }
    }

    /**
     * 构建带签名头像的医生列表副本，避免污染缓存数据
     */
    private List<Doctor> buildDoctorResponseList(List<Doctor> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<Doctor> result = new ArrayList<>(source.size());
        for (Doctor doctor : source) {
            Doctor copy = cloneDoctorWithSignedAvatar(doctor);
            if (copy != null) {
                result.add(copy);
            }
        }
        return result;
    }

    /**
     * 克隆医生对象并附加可访问的头像URL
     */
    private Doctor cloneDoctorWithSignedAvatar(Doctor original) {
        if (original == null) {
            return null;
        }
        Doctor copy = new Doctor();
        BeanUtils.copyProperties(original, copy);
        copy.setAvatar(resolveDoctorAvatar(original));
        return copy;
    }

    /**
     * 生成可直接访问的医生头像
     */
    private String resolveDoctorAvatar(Doctor doctor) {
        if (doctor == null) {
            return avatarConfig.getDefaultDoctor();
        }
        String rawAvatar = doctor.getAvatar();
        if (StringUtils.hasText(rawAvatar)) {
            // 已经是签名地址，直接返回
            if (rawAvatar.contains("Signature=")) {
                return rawAvatar;
            }
            try {
                String signedUrl = ossService.generatePresignedUrl(rawAvatar, avatarConfig.getTtlMinutes());
                if (StringUtils.hasText(signedUrl)) {
                    return signedUrl;
                }
            } catch (Exception e) {
                log.warn("生成医生头像签名失败: doctorId={}, error={}", doctor.getId(), e.getMessage());
                return rawAvatar;
            }
        }
        Long doctorId = doctor.getId();
        if (doctorId != null) {
            return avatarConfig.getDefaultDoctor() + "&seed=" + doctorId;
        }
        return avatarConfig.getDefaultDoctor();
    }

    /**
     * 丰富医生信息（关联科室和用户信息）
     */
    private void enrichDoctorInfo(Doctor doctor) {
        // 关联分类名称
        if (doctor.getCategoryId() != null) {
            try {
                com.hospital.entity.Department department = departmentMapper.selectById(doctor.getCategoryId());
                if (department != null) {
                    doctor.setCategoryName(department.getCategoryName());
                    doctor.setDeptName(department.getCategoryName());
                }
            } catch (Exception e) {
                log.warn("获取分类名称失败: categoryId={}", doctor.getCategoryId(), e);
            }
        }
        // 设置兼容字段
        if (doctor.getCategoryId() != null && doctor.getDeptId() == null) {
            doctor.setDeptId(doctor.getCategoryId());
        }

        // 关联用户信息
        if (doctor.getUserId() != null) {
            try {
                User user = userMapper.selectById(doctor.getUserId());
                if (user != null) {
                    doctor.setGender(user.getGender());
                    doctor.setBirthday(user.getBirthDate());
                    doctor.setAvatar(user.getAvatar());
                    doctor.setPhone(user.getPhone());
                    if (!StringUtils.hasText(doctor.getDoctorName())) {
                        doctor.setDoctorName(user.getRealName());
                    }
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败: userId={}", doctor.getUserId(), e);
            }
        }
    }
}

