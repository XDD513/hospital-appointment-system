package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.DepartmentCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DepartmentCategoryMapper extends BaseMapper<DepartmentCategory> {

    @Select("SELECT * FROM tcm_category ORDER BY sort_order ASC")
    List<DepartmentCategory> selectAll();

    @Select("SELECT * FROM tcm_category WHERE status = 1 ORDER BY sort_order ASC")
    List<DepartmentCategory> selectEnabled();
}