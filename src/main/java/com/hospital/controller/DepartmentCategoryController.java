package com.hospital.controller;

import com.hospital.common.result.Result;
import com.hospital.entity.DepartmentCategory;
import com.hospital.service.DepartmentCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/department/category")
public class DepartmentCategoryController {

    @Autowired
    private DepartmentCategoryService categoryService;

    /**
     * 查询全部分类列表
     */
    @GetMapping("/list")
    public Result<List<DepartmentCategory>> listAll() {
        log.info("查询科室分类列表");
        return categoryService.getCategoryList();
    }

    /**
     * 查询启用分类列表（患者端）
     */
    @GetMapping("/list/enabled")
    public Result<List<DepartmentCategory>> listEnabled() {
        log.info("查询启用科室分类列表");
        return categoryService.getEnabledCategoryList();
    }
}