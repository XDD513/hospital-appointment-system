package com.hospital.service.impl;

import com.hospital.common.result.Result;
import com.hospital.entity.DepartmentCategory;
import com.hospital.mapper.DepartmentCategoryMapper;
import com.hospital.service.DepartmentCategoryService;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DepartmentCategoryServiceImpl implements DepartmentCategoryService {

    @Autowired
    private DepartmentCategoryMapper categoryMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public Result<List<DepartmentCategory>> getCategoryList() {
        try {
            String cacheKey = "hospital:common:dept:category:list";
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<DepartmentCategory> list = (List<DepartmentCategory>) cached;
                    return Result.success(list);
                } catch (ClassCastException ignored) {}
            }
            List<DepartmentCategory> categories = categoryMapper.selectAll();
            // 设置兼容字段
            categories.forEach(this::setCompatibilityFields);
            redisUtil.set(cacheKey, categories);
            return Result.success(categories);
        } catch (Exception e) {
            log.error("查询分类列表失败", e);
            return Result.error("查询分类列表失败");
        }
    }

    @Override
    public Result<List<DepartmentCategory>> getEnabledCategoryList() {
        try {
            String cacheKey = "hospital:common:dept:category:list:enabled";
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<DepartmentCategory> list = (List<DepartmentCategory>) cached;
                    return Result.success(list);
                } catch (ClassCastException ignored) {}
            }
            List<DepartmentCategory> categories = categoryMapper.selectEnabled();
            // 设置兼容字段
            categories.forEach(this::setCompatibilityFields);
            redisUtil.set(cacheKey, categories);
            return Result.success(categories);
        } catch (Exception e) {
            log.error("查询启用分类失败", e);
            return Result.error("查询启用分类失败");
        }
    }

    /**
     * 设置兼容字段（从数据库字段到兼容字段）
     */
    private void setCompatibilityFields(DepartmentCategory category) {
        if (category == null) {
            return;
        }
        // categoryDesc -> description
        category.setDescription(category.getCategoryDesc());
    }
}