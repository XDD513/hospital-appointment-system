package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import com.hospital.entity.HealthCheckin;
import com.hospital.entity.HealthPlanRecord;
import com.hospital.entity.User;
import com.hospital.entity.UserConstitutionTest;
import com.hospital.entity.UserHealthProfile;
import com.hospital.mapper.HealthCheckinMapper;
import com.hospital.mapper.HealthPlanRecordMapper;
import com.hospital.mapper.UserConstitutionTestMapper;
import com.hospital.mapper.UserHealthProfileMapper;
import com.hospital.mapper.UserMapper;
import com.hospital.service.HealthProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 健康档案服务实现类
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Slf4j
@Service
public class HealthProfileServiceImpl implements HealthProfileService {

    @Autowired
    private UserHealthProfileMapper healthProfileMapper;

    @Autowired
    private HealthPlanRecordMapper healthPlanMapper;

    @Autowired
    private HealthCheckinMapper healthCheckinMapper;

    @Autowired
    private UserConstitutionTestMapper constitutionTestMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private com.hospital.util.RedisUtil redisUtil;

    /**
     * 获取用户健康档案
     */
    @Override
    public Result<Map<String, Object>> getHealthProfile(Long userId) {
        try {
            // 1. 尝试从缓存获取
            String cacheKey = "hospital:patient:health:profile:user:" + userId;
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof Map) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cachedProfile = (Map<String, Object>) cached;
                    log.info("从缓存获取健康档案: userId={}", userId);
                    return Result.success(cachedProfile);
                } catch (ClassCastException ignored) {}
            }

            // 2. 查询健康档案
            UserHealthProfile profile = healthProfileMapper.selectByUserId(userId);
            if (profile == null) {
                // 如果不存在，创建默认档案
                profile = new UserHealthProfile();
                profile.setUserId(userId);
                healthProfileMapper.insert(profile);
            }

            // 3. 查询用户基本信息
            User user = userMapper.selectById(userId);

            // 3. 查询最新体质测试结果
            UserConstitutionTest latestTest = constitutionTestMapper.selectLatestByUserId(userId);

            // 4. 组装返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("userName", user != null ? user.getRealName() : null);
            result.put("gender", user != null ? user.getGender() : null);
            result.put("birthday", user != null ? user.getBirthDate() : null);

            // 计算年龄
            if (user != null && user.getBirthDate() != null) {
                int age = LocalDate.now().getYear() - user.getBirthDate().getYear();
                result.put("age", age);
            } else {
                result.put("age", null);
            }

            // 健康档案信息
            result.put("height", profile.getHeight());
            result.put("weight", profile.getWeight());
            result.put("bmi", profile.getBmi());
            result.put("bloodType", profile.getBloodType());
            result.put("allergyHistory", profile.getAllergies());
            result.put("medicalHistory", profile.getMedicalHistory());
            result.put("familyHistory", profile.getFamilyHistory());
            result.put("currentMedications", profile.getCurrentMedications());
            result.put("lifestyle", profile.getLifestyle());
            result.put("dietPreference", profile.getDietPreference());
            result.put("exerciseHabit", profile.getExerciseHabit());
            result.put("sleepQuality", profile.getSleepQuality());
            result.put("stressLevel", profile.getStressLevel());
            result.put("lastCheckupDate", profile.getLastCheckupDate());
            result.put("checkupReport", profile.getCheckupReport());
            result.put("healthGoals", profile.getHealthGoals());
            result.put("remark", profile.getRemark());
            result.put("updatedAt", profile.getUpdatedAt());

            // 体质类型
            if (latestTest != null) {
                result.put("constitutionType", latestTest.getPrimaryConstitution());
                result.put("secondaryConstitutionType", latestTest.getSecondaryConstitution());
            } else {
                result.put("constitutionType", null);
                result.put("secondaryConstitutionType", null);
            }

            // 存入缓存（30分钟）
            redisUtil.set(cacheKey, result, 30, java.util.concurrent.TimeUnit.MINUTES);

            log.info("查询用户健康档案：用户ID={}", userId);
            return Result.success(result);

        } catch (Exception e) {
            log.error("查询用户健康档案失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 更新用户健康档案
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<UserHealthProfile> updateHealthProfile(UserHealthProfile profile) {
        try {
            // 计算BMI
            if (profile.getHeight() != null && profile.getWeight() != null) {
                double heightInMeters = profile.getHeight() / 100.0;
                double bmi = profile.getWeight() / (heightInMeters * heightInMeters);
                profile.setBmi(Math.round(bmi * 10.0) / 10.0);
            }

            UserHealthProfile existingProfile = healthProfileMapper.selectByUserId(profile.getUserId());
            if (existingProfile == null) {
                healthProfileMapper.insert(profile);
            } else {
                profile.setId(existingProfile.getId());
                healthProfileMapper.updateById(profile);
            }

            // 失效缓存
            redisUtil.delete("hospital:patient:health:profile:user:" + profile.getUserId());

            log.info("更新用户健康档案成功：用户ID={}", profile.getUserId());
            return Result.success(profile);

        } catch (Exception e) {
            log.error("更新用户健康档案失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 创建健康计划
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<HealthPlanRecord> createHealthPlan(HealthPlanRecord plan) {
        try {
            // 设置初始值
            plan.setCompletedCount(0);
            plan.setCompletionRate(0.0);
            plan.setStatus(1); // 进行中

            // 计算目标次数
            if (plan.getTargetCount() == null) {
                long days = ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
                plan.setTargetCount((int) days);
            }

            healthPlanMapper.insert(plan);

            log.info("创建健康计划成功：用户ID={}，计划名称={}", plan.getUserId(), plan.getPlanName());
            return Result.success(plan);

        } catch (Exception e) {
            log.error("创建健康计划失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 更新健康计划
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<HealthPlanRecord> updateHealthPlan(HealthPlanRecord plan) {
        try {
            HealthPlanRecord existingPlan = healthPlanMapper.selectById(plan.getId());
            if (existingPlan == null) {
                return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "健康计划不存在");
            }

            // 只允许用户更新自己的计划
            if (!existingPlan.getUserId().equals(plan.getUserId())) {
                return Result.error(ResultCode.FORBIDDEN.getCode(), "无权限更新此计划");
            }

            healthPlanMapper.updateById(plan);

            log.info("更新健康计划成功：计划ID={}", plan.getId());
            return Result.success(plan);

        } catch (Exception e) {
            log.error("更新健康计划失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 删除健康计划
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteHealthPlan(Long id, Long userId) {
        try {
            HealthPlanRecord plan = healthPlanMapper.selectById(id);
            if (plan == null) {
                return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "健康计划不存在");
            }

            // 只允许用户删除自己的计划
            if (!plan.getUserId().equals(userId)) {
                return Result.error(ResultCode.FORBIDDEN.getCode(), "无权限删除此计划");
            }

            // 软删除：更新状态为已放弃
            plan.setStatus(3);
            healthPlanMapper.updateById(plan);

            log.info("删除健康计划成功：计划ID={}", id);
            return Result.success();

        } catch (Exception e) {
            log.error("删除健康计划失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 分页查询用户的健康计划
     */
    @Override
    public Result<IPage<HealthPlanRecord>> getHealthPlanList(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        try {
            Page<HealthPlanRecord> page = new Page<>(pageNum, pageSize);
            IPage<HealthPlanRecord> result = healthPlanMapper.selectByUserId(page, userId, status);
            log.info("查询用户健康计划：用户ID={}，状态={}，共{}条", userId, status, result.getTotal());
            return Result.success(result);

        } catch (Exception e) {
            log.error("查询用户健康计划失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 健康打卡
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<HealthCheckin> healthCheckin(HealthCheckin checkin) {
        try {
            healthCheckinMapper.insert(checkin);

            // 如果关联了计划，更新计划完成次数
            if (checkin.getPlanId() != null) {
                healthPlanMapper.incrementCompletedCount(checkin.getPlanId());
            }

            log.info("健康打卡成功：用户ID={}，打卡类型={}", checkin.getUserId(), checkin.getCheckinType());
            return Result.success(checkin);

        } catch (Exception e) {
            log.error("健康打卡失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 分页查询用户的打卡记录
     */
    @Override
    public Result<IPage<HealthCheckin>> getCheckinList(Long userId, String checkinType, LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize) {
        try {
            // 如果指定了日期范围，使用日期范围查询
            if (startDate != null && endDate != null) {
                List<HealthCheckin> checkins = healthCheckinMapper.selectByDateRange(userId, startDate, endDate);

                // 如果指定了打卡类型，进行过滤
                if (checkinType != null && !checkinType.isEmpty()) {
                    checkins = checkins.stream()
                            .filter(c -> checkinType.equals(c.getCheckinType()))
                            .collect(java.util.stream.Collectors.toList());
                }

                // 手动分页
                Page<HealthCheckin> page = new Page<>(pageNum, pageSize);
                page.setTotal(checkins.size());
                int start = (int) ((pageNum - 1) * pageSize);
                int end = Math.min(start + pageSize, checkins.size());
                page.setRecords(start < checkins.size() ? checkins.subList(start, end) : new java.util.ArrayList<>());

                log.info("查询用户打卡记录（日期范围）：用户ID={}，类型={}，日期范围={} ~ {}，共{}条",
                        userId, checkinType, startDate, endDate, checkins.size());
                return Result.success(page);
            } else {
                // 使用原有的分页查询
                Page<HealthCheckin> page = new Page<>(pageNum, pageSize);
                IPage<HealthCheckin> result = healthCheckinMapper.selectByUserId(page, userId, checkinType);
                log.info("查询用户打卡记录：用户ID={}，类型={}，共{}条", userId, checkinType, result.getTotal());
                return Result.success(result);
            }

        } catch (Exception e) {
            log.error("查询用户打卡记录失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 查询用户指定日期的打卡记录
     */
    @Override
    public Result<HealthCheckin> getCheckinByDate(Long userId, LocalDate date) {
        try {
            List<HealthCheckin> checkins = healthCheckinMapper.selectByUserIdAndDate(userId, date);
            HealthCheckin checkin = checkins.isEmpty() ? null : checkins.get(0);
            log.info("查询用户指定日期打卡记录：用户ID={}，日期={}", userId, date);
            return Result.success(checkin);

        } catch (Exception e) {
            log.error("查询用户指定日期打卡记录失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取用户健康统计数据
     */
    @Override
    public Result<Map<String, Object>> getHealthStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            Map<String, Object> statistics = new HashMap<>();

            // 查询打卡记录
            List<HealthCheckin> checkins = healthCheckinMapper.selectByDateRange(userId, startDate, endDate);

            // 统计打卡天数
            long checkinDays = checkins.stream()
                    .map(HealthCheckin::getCheckinDate)
                    .distinct()
                    .count();
            statistics.put("checkinDays", checkinDays);

            // 统计连续打卡天数
            Integer continuousDays = healthCheckinMapper.countContinuousDays(userId);
            statistics.put("continuousDays", continuousDays);

            // 统计平均体重
            Double avgWeight = checkins.stream()
                    .filter(c -> c.getWeight() != null)
                    .mapToDouble(HealthCheckin::getWeight)
                    .average()
                    .orElse(0.0);
            statistics.put("avgWeight", Math.round(avgWeight * 10.0) / 10.0);

            // 统计平均睡眠时长
            Double avgSleep = checkins.stream()
                    .filter(c -> c.getSleepDuration() != null)
                    .mapToDouble(HealthCheckin::getSleepDuration)
                    .average()
                    .orElse(0.0);
            statistics.put("avgSleepDuration", Math.round(avgSleep * 10.0) / 10.0);

            // 统计平均运动时长
            Double avgExercise = checkins.stream()
                    .filter(c -> c.getExerciseDuration() != null)
                    .mapToDouble(HealthCheckin::getExerciseDuration)
                    .average()
                    .orElse(0.0);
            statistics.put("avgExerciseDuration", Math.round(avgExercise));

            // 统计平均心情评分
            Double avgMood = checkins.stream()
                    .filter(c -> c.getMoodScore() != null)
                    .mapToDouble(HealthCheckin::getMoodScore)
                    .average()
                    .orElse(0.0);
            statistics.put("avgMoodScore", Math.round(avgMood * 10.0) / 10.0);

            // 生成趋势数据
            List<Map<String, Object>> weightTrend = new ArrayList<>();
            List<Map<String, Object>> sleepTrend = new ArrayList<>();
            List<Map<String, Object>> exerciseTrend = new ArrayList<>();

            // 按日期分组统计
            Map<LocalDate, List<HealthCheckin>> checkinsByDate = checkins.stream()
                    .collect(Collectors.groupingBy(HealthCheckin::getCheckinDate));

            // 生成每日趋势数据
            checkinsByDate.forEach((date, dailyCheckins) -> {
                // 体重趋势
                dailyCheckins.stream()
                        .filter(c -> c.getWeight() != null)
                        .findFirst()
                        .ifPresent(c -> {
                            Map<String, Object> weightData = new HashMap<>();
                            weightData.put("date", date.toString());
                            weightData.put("weight", c.getWeight());
                            weightTrend.add(weightData);
                        });

                // 睡眠趋势
                dailyCheckins.stream()
                        .filter(c -> c.getSleepDuration() != null)
                        .findFirst()
                        .ifPresent(c -> {
                            Map<String, Object> sleepData = new HashMap<>();
                            sleepData.put("date", date.toString());
                            sleepData.put("duration", c.getSleepDuration());
                            sleepData.put("quality", c.getSleepQuality() != null ? c.getSleepQuality() : 0);
                            sleepTrend.add(sleepData);
                        });

                // 运动趋势
                dailyCheckins.stream()
                        .filter(c -> c.getExerciseDuration() != null)
                        .findFirst()
                        .ifPresent(c -> {
                            Map<String, Object> exerciseData = new HashMap<>();
                            exerciseData.put("date", date.toString());
                            exerciseData.put("duration", c.getExerciseDuration());
                            exerciseTrend.add(exerciseData);
                        });
            });

            // 按日期排序
            weightTrend.sort((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")));
            sleepTrend.sort((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")));
            exerciseTrend.sort((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")));

            statistics.put("weightTrend", weightTrend);
            statistics.put("sleepTrend", sleepTrend);
            statistics.put("exerciseTrend", exerciseTrend);

            // 心情分布统计
            Map<String, Long> moodDistribution = new HashMap<>();
            moodDistribution.put("GOOD", checkins.stream().filter(c -> c.getMoodScore() != null && c.getMoodScore() >= 4).count());
            moodDistribution.put("NORMAL", checkins.stream().filter(c -> c.getMoodScore() != null && c.getMoodScore() == 3).count());
            moodDistribution.put("BAD", checkins.stream().filter(c -> c.getMoodScore() != null && c.getMoodScore() <= 2).count());
            statistics.put("moodDistribution", moodDistribution);

            log.info("获取用户健康统计数据：用户ID={}，打卡天数={}", userId, checkinDays);
            return Result.success(statistics);

        } catch (Exception e) {
            log.error("获取用户健康统计数据失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 生成用户健康报告
     */
    @Override
    public Result<Map<String, Object>> generateHealthReport(Long userId) {
        try {
            Map<String, Object> report = new HashMap<>();

            // 1. 基本信息
            UserHealthProfile profile = healthProfileMapper.selectByUserId(userId);
            report.put("profile", profile);

            // 2. 体质信息
            UserConstitutionTest latestTest = constitutionTestMapper.selectLatestByUserId(userId);
            report.put("constitution", latestTest);

            // 3. 最近30天统计
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Result<Map<String, Object>> statisticsResult = getHealthStatistics(userId, startDate, endDate);
            report.put("statistics", statisticsResult.getData());

            // 4. 进行中的计划
            List<HealthPlanRecord> activePlans = healthPlanMapper.selectActivePlans(userId);
            report.put("activePlans", activePlans);

            // 5. 健康建议
            List<String> suggestions = generateHealthSuggestions(profile, latestTest, statisticsResult.getData());
            report.put("suggestions", suggestions);

            log.info("生成用户健康报告成功：用户ID={}", userId);
            return Result.success(report);

        } catch (Exception e) {
            log.error("生成用户健康报告失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 生成健康建议
     */
    private List<String> generateHealthSuggestions(UserHealthProfile profile, UserConstitutionTest constitution, Map<String, Object> statistics) {
        List<String> suggestions = new ArrayList<>();

        // BMI建议
        if (profile != null && profile.getBmi() != null) {
            double bmi = profile.getBmi();
            if (bmi < 18.5) {
                suggestions.add("您的BMI偏低，建议增加营养摄入，适当增重");
            } else if (bmi >= 24 && bmi < 28) {
                suggestions.add("您的BMI偏高，建议控制饮食，增加运动");
            } else if (bmi >= 28) {
                suggestions.add("您的BMI过高，建议咨询医生，制定减重计划");
            }
        }

        // 睡眠建议
        if (statistics != null && statistics.get("avgSleepDuration") != null) {
            double avgSleep = (Double) statistics.get("avgSleepDuration");
            if (avgSleep < 7) {
                suggestions.add("您的平均睡眠时长不足，建议保证每天7-8小时睡眠");
            }
        }

        // 运动建议
        if (statistics != null && statistics.get("avgExerciseDuration") != null) {
            double avgExercise = (Double) statistics.get("avgExerciseDuration");
            if (avgExercise < 30) {
                suggestions.add("您的运动量不足，建议每天至少运动30分钟");
            }
        }

        // 体质建议
        if (constitution != null) {
            String constitutionType = constitution.getPrimaryConstitution();
            suggestions.add("根据您的" + constitutionType + "体质，建议关注相应的养生方案");
        }

        return suggestions;
    }
}

