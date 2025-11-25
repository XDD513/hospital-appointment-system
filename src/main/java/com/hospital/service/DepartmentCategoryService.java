package com.hospital.service;

import com.hospital.common.result.Result;
import com.hospital.entity.DepartmentCategory;

import java.util.List;

public interface DepartmentCategoryService {
    Result<List<DepartmentCategory>> getCategoryList();
    Result<List<DepartmentCategory>> getEnabledCategoryList();
}