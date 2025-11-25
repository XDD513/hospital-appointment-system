package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.ConstitutionType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 体质类型Mapper接口
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Mapper
public interface ConstitutionTypeMapper extends BaseMapper<ConstitutionType> {

    /**
     * 根据体质代码查询体质类型
     *
     * @param typeCode 体质代码
     * @return 体质类型对象
     */
    @Select("SELECT * FROM constitution_type WHERE type_code = #{typeCode}")
    ConstitutionType selectByTypeCode(String typeCode);

    /**
     * 查询所有体质类型（按排序顺序）
     *
     * @return 体质类型列表
     */
    @Select("SELECT * FROM constitution_type ORDER BY sort_order ASC")
    List<ConstitutionType> selectAllOrdered();
}

