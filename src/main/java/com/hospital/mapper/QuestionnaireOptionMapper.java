package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.QuestionnaireOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 问卷选项Mapper接口
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Mapper
public interface QuestionnaireOptionMapper extends BaseMapper<QuestionnaireOption> {

    /**
     * 根据问题ID查询所有选项（按顺序）
     *
     * @param questionId 问题ID
     * @return 选项列表
     */
    @Select("SELECT * FROM questionnaire_option WHERE question_id = #{questionId} ORDER BY option_order ASC")
    List<QuestionnaireOption> selectByQuestionId(Long questionId);

    /**
     * 批量查询多个问题的选项
     *
     * @param questionIds 问题ID列表
     * @return 选项列表
     */
    @Select("<script>" +
            "SELECT * FROM questionnaire_option WHERE question_id IN " +
            "<foreach collection='questionIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " ORDER BY question_id, option_order ASC" +
            "</script>")
    List<QuestionnaireOption> selectByQuestionIds(List<Long> questionIds);
}

