package com.hospital.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;

/**
 * 测试结果响应DTO
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Data
public class TestResultResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 测试记录ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 主要体质代码
     */
    private String primaryConstitution;

    /**
     * 主要体质名称
     */
    private String primaryConstitutionName;

    /**
     * 次要体质代码
     */
    private String secondaryConstitution;

    /**
     * 次要体质名称
     */
    private String secondaryConstitutionName;

    /**
     * 各体质得分（体质代码 -> 转化分）
     */
    private Map<String, Double> scores;

    /**
     * 测试日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate testDate;

    /**
     * 测试报告
     */
    private String report;

    /**
     * 养生建议
     */
    private String healthSuggestion;

    /**
     * 主要体质详情
     */
    private ConstitutionTypeResponse primaryConstitutionDetail;

    /**
     * 次要体质详情
     */
    private ConstitutionTypeResponse secondaryConstitutionDetail;
}

