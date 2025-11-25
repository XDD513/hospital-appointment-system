package com.hospital.service;

import com.hospital.common.result.Result;
import com.hospital.dto.request.SubmitTestRequest;
import com.hospital.dto.response.QuestionnaireResponse;
import com.hospital.dto.response.TestResultResponse;

import java.util.List;

/**
 * 体质测试服务接口
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
public interface ConstitutionTestService {

    /**
     * 获取完整测试问卷
     *
     * @return 问卷列表（包含选项）
     */
    Result<List<QuestionnaireResponse>> getQuestionnaire();

    /**
     * 提交测试答案并计算结果
     *
     * @param userId 用户ID
     * @param request 测试答案
     * @return 测试结果
     */
    Result<TestResultResponse> submitTest(Long userId, SubmitTestRequest request);

    /**
     * 获取用户测试历史记录
     *
     * @param userId 用户ID
     * @return 测试历史列表
     */
    Result<List<TestResultResponse>> getTestHistory(Long userId);

    /**
     * 获取用户最新测试结果
     *
     * @param userId 用户ID
     * @return 最新测试结果
     */
    Result<TestResultResponse> getLatestTestResult(Long userId);

    /**
     * 根据测试ID获取测试报告
     *
     * @param testId 测试ID
     * @return 测试报告
     */
    Result<TestResultResponse> getTestReport(Long testId);

    /**
     * 根据预约ID获取体质测试结果
     *
     * @param appointmentId 预约ID
     * @return 测试结果
     */
    Result<TestResultResponse> getTestResultByAppointment(Long appointmentId);

    /**
     * 检查预约是否已有体质测试记录
     *
     * @param appointmentId 预约ID
     * @return 是否存在测试记录
     */
    Result<Boolean> hasTestByAppointment(Long appointmentId);
}

