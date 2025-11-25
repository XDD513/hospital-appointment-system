package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.ConstitutionQuestionnaire;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 体质测试问卷Mapper接口
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Mapper
public interface ConstitutionQuestionnaireMapper extends BaseMapper<ConstitutionQuestionnaire> {

    /**
     * 查询所有问卷题目（按顺序）
     *
     * @return 问卷题目列表
     */
    @Select("SELECT * FROM constitution_questionnaire ORDER BY question_order ASC")
    List<ConstitutionQuestionnaire> selectAllEnabled();

    /**
     * 根据体质类型ID查询问卷题目
     *
     * @param constitutionTypeId 体质类型ID
     * @return 问卷题目列表
     */
    @Select("SELECT * FROM constitution_questionnaire WHERE constitution_type_id = #{constitutionTypeId} ORDER BY question_order ASC")
    List<ConstitutionQuestionnaire> selectByConstitutionTypeId(Long constitutionTypeId);
}

