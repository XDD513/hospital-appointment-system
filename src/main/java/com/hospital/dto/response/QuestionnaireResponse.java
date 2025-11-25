package com.hospital.dto.response;

import com.hospital.entity.QuestionnaireOption;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 问卷响应DTO
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Data
public class QuestionnaireResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 问题ID
     */
    private Long id;

    /**
     * 所属体质类型ID
     */
    private Long constitutionTypeId;

    /**
     * 问题文本
     */
    private String questionText;

    /**
     * 问题顺序
     */
    private Integer questionOrder;

    /**
     * 问题分类
     */
    private String category;

    /**
     * 选项列表
     */
    private List<QuestionnaireOption> options;
}

