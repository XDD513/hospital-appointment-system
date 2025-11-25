package com.hospital.controller;

import com.hospital.annotation.OperationLog;
import com.hospital.common.result.Result;
import com.hospital.dto.request.SubmitTestRequest;
import com.hospital.dto.response.ConstitutionTypeResponse;
import com.hospital.dto.response.QuestionnaireResponse;
import com.hospital.dto.response.TestResultResponse;
import com.hospital.service.ConstitutionService;
import com.hospital.service.ConstitutionTestService;
import com.hospital.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 中医体质测试控制器
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Slf4j
@RestController
@RequestMapping("/api/constitution")
public class ConstitutionController {

    @Autowired
    private ConstitutionService constitutionService;

    @Autowired
    private ConstitutionTestService constitutionTestService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取所有体质类型列表
     */
    @GetMapping("/types")
    public Result<List<ConstitutionTypeResponse>> getConstitutionTypes() {
        log.info("获取体质类型列表");
        return constitutionService.getConstitutionTypes();
    }

    /**
     * 根据体质代码获取体质详情
     */
    @GetMapping("/type/{code}")
    public Result<ConstitutionTypeResponse> getConstitutionDetail(@PathVariable("code") String code) {
        log.info("获取体质详情: {}", code);
        return constitutionService.getConstitutionDetail(code);
    }

    /**
     * 获取完整测试问卷
     */
    @GetMapping("/questionnaire")
    public Result<List<QuestionnaireResponse>> getQuestionnaire() {
        log.info("获取体质测试问卷");
        return constitutionTestService.getQuestionnaire();
    }

    /**
     * 提交测试答案
     */
    @OperationLog(module = "CONSTITUTION", type = "INSERT", description = "提交体质测试")
    @PostMapping("/test/submit")
    public Result<TestResultResponse> submitTest(@Validated @RequestBody SubmitTestRequest request,
                                                   HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        log.info("用户{}提交体质测试", userId);
        return constitutionTestService.submitTest(userId, request);
    }

    /**
     * 获取用户测试历史记录
     */
    @GetMapping("/test/history")
    public Result<List<TestResultResponse>> getTestHistory(HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        log.info("获取用户{}的测试历史", userId);
        return constitutionTestService.getTestHistory(userId);
    }

    /**
     * 获取用户最新测试结果
     */
    @GetMapping("/test/latest")
    public Result<TestResultResponse> getLatestTestResult(HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        log.info("获取用户{}的最新测试结果", userId);
        return constitutionTestService.getLatestTestResult(userId);
    }

    /**
     * 根据测试ID获取测试报告
     */
    @GetMapping("/test/report/{id}")
    public Result<TestResultResponse> getTestReport(@PathVariable("id") Long id) {
        log.info("获取测试报告: {}", id);
        return constitutionTestService.getTestReport(id);
    }

    /**
     * 根据用户ID获取最新测试结果（医生端使用）
     */
    @GetMapping("/test/user/{userId}/latest")
    public Result<TestResultResponse> getUserLatestTestResult(@PathVariable("userId") Long userId) {
        log.info("获取用户{}的最新测试结果（医生端查询）", userId);
        return constitutionTestService.getLatestTestResult(userId);
    }

    /**
     * 根据预约ID获取体质测试结果（医生端使用）
     */
    @GetMapping("/test/appointment/{appointmentId}")
    public Result<TestResultResponse> getTestResultByAppointment(@PathVariable("appointmentId") Long appointmentId) {
        log.info("获取预约{}关联的体质测试结果", appointmentId);
        return constitutionTestService.getTestResultByAppointment(appointmentId);
    }

    /**
     * 检查预约是否已有体质测试记录
     */
    @GetMapping("/test/appointment/{appointmentId}/exists")
    public Result<Boolean> checkTestByAppointment(@PathVariable("appointmentId") Long appointmentId) {
        log.info("检查预约{}是否已有测试记录", appointmentId);
        return constitutionTestService.hasTestByAppointment(appointmentId);
    }
}

