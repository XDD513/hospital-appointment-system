package com.hospital.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import com.hospital.dto.request.SubmitTestRequest;
import com.hospital.dto.response.ConstitutionTypeResponse;
import com.hospital.dto.response.QuestionnaireResponse;
import com.hospital.dto.response.TestResultResponse;
import com.hospital.entity.*;
import com.hospital.mapper.*;
import com.hospital.service.ConstitutionTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 体质测试服务实现类
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Slf4j
@Service
public class ConstitutionTestServiceImpl implements ConstitutionTestService {

    @Autowired
    private ConstitutionQuestionnaireMapper questionnaireMapper;

    @Autowired
    private QuestionnaireOptionMapper optionMapper;

    @Autowired
    private UserConstitutionTestMapper testMapper;

    @Autowired
    private ConstitutionTypeMapper constitutionTypeMapper;

    @Autowired
    private com.hospital.util.RedisUtil redisUtil;

    @Autowired
    private com.hospital.mapper.AppointmentMapper appointmentMapper;

    @Autowired
    private com.hospital.mapper.DoctorMapper doctorMapper;

    @Autowired
    private com.hospital.mapper.UserMapper userMapper;

    @Autowired
    private com.hospital.service.NotificationService notificationService;

    /**
     * 获取完整测试问卷
     */
    @Override
    public Result<List<QuestionnaireResponse>> getQuestionnaire() {
        try {
            // 1. 尝试从缓存获取
            String cacheKey = "hospital:common:constitution:questionnaire";
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<QuestionnaireResponse> list = (List<QuestionnaireResponse>) cached;
                    log.info("从缓存获取问卷数据，共{}题", list.size());
                    return Result.success(list);
                } catch (ClassCastException ignored) {}
            }

            // 2. 查询所有启用的问卷题目
            List<ConstitutionQuestionnaire> questions = questionnaireMapper.selectAllEnabled();
            if (questions.isEmpty()) {
                log.warn("问卷题目为空");
                return Result.error(ResultCode.DATA_NOT_FOUND);
            }

            // 3. 查询所有题目的选项
            List<Long> questionIds = questions.stream()
                    .map(ConstitutionQuestionnaire::getId)
                    .collect(Collectors.toList());
            List<QuestionnaireOption> allOptions = optionMapper.selectByQuestionIds(questionIds);

            // 4. 按问题ID分组选项
            Map<Long, List<QuestionnaireOption>> optionsMap = allOptions.stream()
                    .collect(Collectors.groupingBy(QuestionnaireOption::getQuestionId));

            // 5. 组装响应数据
            List<QuestionnaireResponse> responseList = questions.stream()
                    .map(question -> {
                        QuestionnaireResponse response = BeanUtil.copyProperties(question, QuestionnaireResponse.class);
                        response.setOptions(optionsMap.getOrDefault(question.getId(), new ArrayList<>()));
                        return response;
                    })
                    .collect(Collectors.toList());

            // 6. 存入缓存（永久）
            redisUtil.set(cacheKey, responseList);

            log.info("成功获取问卷，共{}题", responseList.size());
            return Result.success(responseList);

        } catch (Exception e) {
            log.error("获取问卷失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 提交测试答案并计算结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TestResultResponse> submitTest(Long userId, SubmitTestRequest request) {
        try {
            // 1. 验证答案数量
            Map<Long, Long> answers = request.getAnswers();
            if (answers.size() != 66) {
                log.warn("答案数量不正确: {}", answers.size());
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "必须完成所有66题");
            }

            // 2. 查询所有问卷题目和选项
            List<ConstitutionQuestionnaire> questions = questionnaireMapper.selectAllEnabled();
            List<Long> questionIds = questions.stream()
                    .map(ConstitutionQuestionnaire::getId)
                    .collect(Collectors.toList());
            List<QuestionnaireOption> allOptions = optionMapper.selectByQuestionIds(questionIds);

            // 3. 构建选项ID到分值的映射
            Map<Long, Integer> optionScoreMap = allOptions.stream()
                    .collect(Collectors.toMap(QuestionnaireOption::getId, QuestionnaireOption::getScore));

            // 4. 按体质类型分组题目
            Map<Long, List<ConstitutionQuestionnaire>> questionsByType = questions.stream()
                    .collect(Collectors.groupingBy(ConstitutionQuestionnaire::getConstitutionTypeId));

            // 5. 计算各体质得分
            Map<String, Double> scores = new HashMap<>();
            List<ConstitutionType> constitutionTypes = constitutionTypeMapper.selectAllOrdered();

            for (ConstitutionType type : constitutionTypes) {
                List<ConstitutionQuestionnaire> typeQuestions = questionsByType.get(type.getId());
                if (typeQuestions == null || typeQuestions.isEmpty()) {
                    continue;
                }

                // 计算原始分
                int rawScore = 0;
                int questionCount = typeQuestions.size();
                
                for (ConstitutionQuestionnaire question : typeQuestions) {
                    Long optionId = answers.get(question.getId());
                    if (optionId != null && optionScoreMap.containsKey(optionId)) {
                        rawScore += optionScoreMap.get(optionId);
                    }
                }

                // 计算转化分：(原始分 - 题目数) / (题目数 × 4) × 100
                double transformedScore = (rawScore - questionCount) / (questionCount * 4.0) * 100;
                transformedScore = Math.round(transformedScore * 100.0) / 100.0; // 保留两位小数
                scores.put(type.getTypeCode(), transformedScore);
            }

            // 6. 判定主要体质和次要体质
            List<Map.Entry<String, Double>> sortedScores = scores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());

            String primaryConstitution = sortedScores.get(0).getKey();
            String secondaryConstitution = null;
            if (sortedScores.size() > 1 && sortedScores.get(1).getValue() >= 60) {
                secondaryConstitution = sortedScores.get(1).getKey();
            }

            // 7. 查询体质类型信息
            ConstitutionType primaryType = constitutionTypeMapper.selectByTypeCode(primaryConstitution);
            ConstitutionType secondaryType = secondaryConstitution != null ?
                    constitutionTypeMapper.selectByTypeCode(secondaryConstitution) : null;

            // 8. 计算总分（转化分的总和）
            int totalScore = (int) scores.values().stream().mapToDouble(Double::doubleValue).sum();

            // 9. 保存测试记录
            UserConstitutionTest test = new UserConstitutionTest();
            test.setUserId(userId);
            test.setAppointmentId(request.getAppointmentId()); // 保存关联的预约ID
            test.setPrimaryConstitution(primaryConstitution);
            test.setSecondaryConstitution(secondaryConstitution);
            test.setTestResult(JSONUtil.toJsonStr(scores));
            test.setAnswers(JSONUtil.toJsonStr(answers));
            test.setTotalScore(totalScore);
            test.setReportGenerated(0);
            test.setTestDate(LocalDateTime.now());

            testMapper.insert(test);

            // 9. 发送通知给医生：体质测试完成
            try {
                if (request.getAppointmentId() != null) {
                    com.hospital.entity.Appointment appointment = appointmentMapper.selectById(request.getAppointmentId());
                    if (appointment != null && appointment.getDoctorId() != null) {
                        com.hospital.entity.Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
                        if (doctor != null && doctor.getUserId() != null) {
                            com.hospital.entity.User patient = userMapper.selectById(userId);
                            String patientName = patient != null && patient.getRealName() != null ? patient.getRealName() : "患者";
                            // 使用体质类型的中文名称而不是代码
                            String constitutionName = primaryType != null && primaryType.getTypeName() != null ? 
                                    primaryType.getTypeName() : primaryConstitution;
                            String content = String.format("患者%s已完成体质测试，主要体质：%s", patientName, constitutionName);
                            notificationService.createAndSendNotification(
                                    doctor.getUserId(),
                                    "体质测试完成通知",
                                    content,
                                    "CONSTITUTION_TEST_COMPLETED"
                            );
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("发送体质测试完成通知失败: userId={}, appointmentId={}, error={}", 
                        userId, request.getAppointmentId(), e.getMessage());
            }

            // 10. 构建响应
            TestResultResponse response = buildTestResultResponse(test, primaryType, secondaryType, scores);

            log.info("用户{}完成体质测试，主要体质：{}，次要体质：{}", userId, primaryConstitution, secondaryConstitution);
            return Result.success(response);

        } catch (Exception e) {
            log.error("提交测试失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取用户测试历史记录
     */
    @Override
    public Result<List<TestResultResponse>> getTestHistory(Long userId) {
        try {
            List<UserConstitutionTest> tests = testMapper.selectHistoryByUserId(userId);
            
            List<TestResultResponse> responseList = tests.stream()
                    .map(test -> {
                        ConstitutionType primaryType = constitutionTypeMapper.selectByTypeCode(test.getPrimaryConstitution());
                        ConstitutionType secondaryType = test.getSecondaryConstitution() != null ?
                                constitutionTypeMapper.selectByTypeCode(test.getSecondaryConstitution()) : null;

                        Map<String, Double> scores = parseScoresFromJson(test.getTestResult());
                        return buildTestResultResponse(test, primaryType, secondaryType, scores);
                    })
                    .collect(Collectors.toList());

            log.info("成功获取用户{}的测试历史，共{}条", userId, responseList.size());
            return Result.success(responseList);

        } catch (Exception e) {
            log.error("获取测试历史失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取用户最新测试结果
     */
    @Override
    public Result<TestResultResponse> getLatestTestResult(Long userId) {
        try {
            UserConstitutionTest test = testMapper.selectLatestByUserId(userId);
            if (test == null) {
                log.warn("用户{}尚未进行体质测试", userId);
                return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "您还没有进行过体质测试");
            }

            ConstitutionType primaryType = constitutionTypeMapper.selectByTypeCode(test.getPrimaryConstitution());
            ConstitutionType secondaryType = test.getSecondaryConstitution() != null ?
                    constitutionTypeMapper.selectByTypeCode(test.getSecondaryConstitution()) : null;

            Map<String, Double> scores = parseScoresFromJson(test.getTestResult());
            TestResultResponse response = buildTestResultResponse(test, primaryType, secondaryType, scores);

            log.info("成功获取用户{}的最新测试结果", userId);
            return Result.success(response);

        } catch (Exception e) {
            log.error("获取最新测试结果失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 根据测试ID获取测试报告
     */
    @Override
    public Result<TestResultResponse> getTestReport(Long testId) {
        try {
            UserConstitutionTest test = testMapper.selectById(testId);
            if (test == null) {
                log.warn("测试记录不存在: {}", testId);
                return Result.error(ResultCode.DATA_NOT_FOUND);
            }

            ConstitutionType primaryType = constitutionTypeMapper.selectByTypeCode(test.getPrimaryConstitution());
            ConstitutionType secondaryType = test.getSecondaryConstitution() != null ?
                    constitutionTypeMapper.selectByTypeCode(test.getSecondaryConstitution()) : null;

            Map<String, Double> scores = parseScoresFromJson(test.getTestResult());
            TestResultResponse response = buildTestResultResponse(test, primaryType, secondaryType, scores);

            log.info("成功获取测试报告: {}", testId);
            return Result.success(response);

        } catch (Exception e) {
            log.error("获取测试报告失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从JSON字符串解析分数Map，确保值为Double类型
     */
    private Map<String, Double> parseScoresFromJson(String jsonStr) {
        Map<String, Object> rawMap = JSONUtil.toBean(jsonStr, Map.class);
        Map<String, Double> scores = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            Object value = entry.getValue();
            Double score;

            if (value instanceof Double) {
                score = (Double) value;
            } else if (value instanceof Integer) {
                score = ((Integer) value).doubleValue();
            } else if (value instanceof Number) {
                score = ((Number) value).doubleValue();
            } else {
                score = Double.parseDouble(value.toString());
            }

            scores.put(entry.getKey(), score);
        }

        return scores;
    }

    /**
     * 构建测试结果响应
     */
    private TestResultResponse buildTestResultResponse(UserConstitutionTest test,
                                                        ConstitutionType primaryType,
                                                        ConstitutionType secondaryType,
                                                        Map<String, Double> scores) {
        TestResultResponse response = new TestResultResponse();
        response.setId(test.getId());
        response.setPrimaryConstitution(test.getPrimaryConstitution());
        response.setPrimaryConstitutionName(primaryType.getTypeName());
        response.setSecondaryConstitution(test.getSecondaryConstitution());
        response.setSecondaryConstitutionName(secondaryType != null ? secondaryType.getTypeName() : null);
        response.setScores(scores);

        // 将 LocalDateTime 转换为 LocalDate
        response.setTestDate(test.getTestDate() != null ? test.getTestDate().toLocalDate() : null);

        // 动态生成报告和建议
        response.setReport(generateReportText(primaryType, secondaryType, scores));
        response.setHealthSuggestion(generateHealthSuggestionText(primaryType, secondaryType));

        response.setPrimaryConstitutionDetail(BeanUtil.copyProperties(primaryType, ConstitutionTypeResponse.class));
        response.setSecondaryConstitutionDetail(secondaryType != null ?
                BeanUtil.copyProperties(secondaryType, ConstitutionTypeResponse.class) : null);

        return response;
    }

    /**
     * 生成测试报告文本
     */
    private String generateReportText(ConstitutionType primaryType, ConstitutionType secondaryType, Map<String, Double> scores) {
        StringBuilder report = new StringBuilder();
        report.append("【体质测试报告】\n\n");
        report.append("您的主要体质是：").append(primaryType.getTypeName()).append("\n");
        report.append("得分：").append(String.format("%.1f", scores.get(primaryType.getTypeCode()))).append("分\n\n");

        if (secondaryType != null) {
            report.append("您的次要体质是：").append(secondaryType.getTypeName()).append("\n");
            report.append("得分：").append(String.format("%.1f", scores.get(secondaryType.getTypeCode()))).append("分\n\n");
        }

        report.append("【主要特征】\n").append(primaryType.getCharacteristics()).append("\n\n");
        report.append("【易患疾病】\n").append(primaryType.getSusceptibleDiseases()).append("\n");

        return report.toString();
    }

    /**
     * 生成养生建议文本
     */
    private String generateHealthSuggestionText(ConstitutionType primaryType, ConstitutionType secondaryType) {
        StringBuilder suggestion = new StringBuilder();
        suggestion.append("【养生建议】\n\n");
        suggestion.append("饮食调养：\n").append(primaryType.getDietAdvice()).append("\n\n");
        suggestion.append("运动调养：\n").append(primaryType.getExerciseAdvice()).append("\n\n");
        suggestion.append("情志调节：\n").append(primaryType.getEmotionAdvice()).append("\n");

        if (secondaryType != null) {
            suggestion.append("\n【次要体质建议】\n");
            suggestion.append("饮食：").append(secondaryType.getDietAdvice()).append("\n");
        }

        return suggestion.toString();
    }

    /**
     * 根据预约ID获取体质测试结果
     */
    @Override
    public Result<TestResultResponse> getTestResultByAppointment(Long appointmentId) {
        try {
            UserConstitutionTest test = testMapper.selectByAppointmentId(appointmentId);
            if (test == null) {
                log.warn("预约{}没有关联的体质测试记录", appointmentId);
                return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "该预约没有关联的体质测试记录");
            }

            ConstitutionType primaryType = constitutionTypeMapper.selectByTypeCode(test.getPrimaryConstitution());
            ConstitutionType secondaryType = test.getSecondaryConstitution() != null ?
                    constitutionTypeMapper.selectByTypeCode(test.getSecondaryConstitution()) : null;

            Map<String, Double> scores = JSONUtil.toBean(test.getTestResult(), new TypeReference<Map<String, Double>>() {}, false);

            TestResultResponse response = buildTestResultResponse(test, primaryType, secondaryType, scores);

            log.info("成功获取预约{}的体质测试结果", appointmentId);
            return Result.success(response);

        } catch (Exception e) {
            log.error("获取预约体质测试结果失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 检查预约是否已有体质测试记录
     */
    @Override
    public Result<Boolean> hasTestByAppointment(Long appointmentId) {
        try {
            UserConstitutionTest test = testMapper.selectByAppointmentId(appointmentId);
            boolean exists = test != null;
            log.info("检查预约{}是否有测试记录: {}", appointmentId, exists);
            return Result.success(exists);
        } catch (Exception e) {
            log.error("检查预约测试记录失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }
}

