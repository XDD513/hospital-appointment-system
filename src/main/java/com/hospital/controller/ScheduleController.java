package com.hospital.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.annotation.OperationLog;
import com.hospital.common.constant.CacheConstants;
import com.hospital.common.constant.SystemConstants;
import com.hospital.common.result.Result;
import com.hospital.entity.Schedule;
import com.hospital.entity.Doctor;
import com.hospital.entity.Department;
import com.hospital.service.ScheduleService;
import com.hospital.dto.request.BatchCreateScheduleRequest;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.DepartmentMapper;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 排班管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;
    
    @Autowired
    private DoctorMapper doctorMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 创建排班
     */
    @OperationLog(module = "SCHEDULE", type = "INSERT", description = "创建排班")
    @PostMapping("/create")
    public Result<Boolean> createSchedule(@RequestBody Schedule schedule) {
        log.info("创建排班请求，数据：{}", schedule);

        try {
            // 数据验证
            if (schedule.getDoctorId() == null) {
                return Result.error("医生ID不能为空");
            }
            if (schedule.getScheduleDate() == null) {
                return Result.error("排班日期不能为空");
            }
            if (schedule.getTimeSlot() == null || schedule.getTimeSlot().isEmpty()) {
                return Result.error("时段不能为空");
            }

            boolean result = scheduleService.save(schedule);
            log.info("创建排班结果：{}", result ? "成功" : "失败");

            if (result) {
                // 失效管理员列表缓存
                redisUtil.deleteByPattern("hospital:admin:schedule:list:*");
                log.info("已失效排班列表缓存");
                return Result.success(true);
            } else {
                return Result.error("创建排班失败");
            }
        } catch (Exception e) {
            log.error("创建排班异常", e);
            return Result.error("创建排班失败：" + e.getMessage());
        }
    }

    /**
     * 批量创建排班（支持多个医生、日期范围、多个时段）
     */
    @OperationLog(module = "SCHEDULE", type = "INSERT", description = "批量创建排班")
    @PostMapping("/batch/create")
    public Result<Map<String, Object>> batchCreateSchedules(@RequestBody BatchCreateScheduleRequest request) {
        log.info("批量创建排班，请求: {}", request);
        Result<Map<String, Object>> result = scheduleService.batchCreateSchedules(request);
        if (result.getCode() == 200) {
            // 失效管理员列表缓存
            redisUtil.deleteByPattern("hospital:admin:schedule:list:*");
            log.info("已失效排班列表缓存");
            return result;
        }
        return Result.error(result.getMessage());
    }

    /**
     * 查询排班列表
     */
    @GetMapping("/list")
    public Result<IPage<Schedule>> getScheduleList(@RequestParam Map<String, Object> params) {
        log.info("查询排班列表，参数：{}", params);
        QueryWrapper<Schedule> wrapper = new QueryWrapper<>();

        // 科室筛选（通过医生ID间接筛选）
        if (params.containsKey("deptId") && params.get("deptId") != null && !params.get("deptId").toString().isEmpty()) {
            try {
                Long deptId = Long.parseLong(params.get("deptId").toString());
                // 先查询该中医分类下的所有医生ID
                List<Doctor> doctorsInDept = doctorMapper.selectList(
                    new QueryWrapper<Doctor>().eq("category_id", deptId)
                );
                if (!doctorsInDept.isEmpty()) {
                    List<Long> doctorIds = doctorsInDept.stream()
                        .map(Doctor::getId)
                        .collect(Collectors.toList());
                    wrapper.in("doctor_id", doctorIds);
                } else {
                    // 如果分类下没有医生，返回空结果
                    wrapper.eq("doctor_id", -1L); // 不存在的医生ID
                }
            } catch (NumberFormatException e) {
                log.warn("无效的科室ID参数：{}", params.get("deptId"));
            }
        }

        // 医生筛选
        if (params.containsKey("doctorId") && params.get("doctorId") != null && !params.get("doctorId").toString().isEmpty()) {
            try {
                Long doctorId = Long.parseLong(params.get("doctorId").toString());
                wrapper.eq("doctor_id", doctorId);
            } catch (NumberFormatException e) {
                log.warn("无效的医生ID参数：{}", params.get("doctorId"));
            }
        }

        // 日期范围筛选
        if (params.containsKey("startDate") && StringUtils.hasText((String) params.get("startDate"))) {
            wrapper.ge("schedule_date", params.get("startDate"));
        }
        if (params.containsKey("endDate") && StringUtils.hasText((String) params.get("endDate"))) {
            wrapper.le("schedule_date", params.get("endDate"));
        }

        // 按排班日期从小到大排序，相同日期按时段排序
        wrapper.orderByAsc("schedule_date", "time_slot");

        // 安全地获取分页参数
        Integer page = 1;
        Integer pageSize = SystemConstants.DEFAULT_PAGE_SIZE;
        if (params.get("page") != null) {
            try {
                page = Integer.parseInt(params.get("page").toString());
            } catch (NumberFormatException e) {
                log.warn("无效的页码参数：{}", params.get("page"));
            }
        }
        if (params.get("pageSize") != null) {
            try {
                pageSize = Integer.parseInt(params.get("pageSize").toString());
            } catch (NumberFormatException e) {
                log.warn("无效的页面大小参数：{}", params.get("pageSize"));
            }
        }

        // 构造缓存键（仅缓存前2页）
        Map<String, Object> filterParams = new HashMap<>();
        if (params.containsKey("deptId") && params.get("deptId") != null) {
            filterParams.put("deptId", params.get("deptId"));
        }
        if (params.containsKey("doctorId") && params.get("doctorId") != null) {
            filterParams.put("doctorId", params.get("doctorId"));
        }
        if (params.containsKey("startDate") && StringUtils.hasText((String) params.get("startDate"))) {
            filterParams.put("startDate", params.get("startDate"));
        }
        if (params.containsKey("endDate") && StringUtils.hasText((String) params.get("endDate"))) {
            filterParams.put("endDate", params.get("endDate"));
        }

        String cacheKey = redisUtil.buildCacheKey("hospital:admin:schedule:list", page, pageSize, filterParams);

        // 仅缓存前2页
        if (page <= CacheConstants.ADMIN_SCHEDULE_LIST_HOT_PAGES) {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                IPage<Schedule> cachedPage = (IPage<Schedule>) cached;
                log.info("从缓存获取排班列表: page={}, pageSize={}", page, pageSize);
                return Result.success(cachedPage);
            }
        }
        
        Page<Schedule> pageObject = new Page<>(page, pageSize);

        IPage<Schedule> schedules = scheduleService.page(pageObject, wrapper);
        
        // 批量关联医生和科室信息（避免N+1查询）
        List<Schedule> scheduleList = schedules.getRecords();
        if (!scheduleList.isEmpty()) {
            // 获取所有医生ID
            List<Long> doctorIds = scheduleList.stream()
                .map(Schedule::getDoctorId)
                .distinct()
                .collect(Collectors.toList());
            
            // 批量查询医生信息
            List<Doctor> doctors = doctorMapper.selectBatchIds(doctorIds);
            Map<Long, Doctor> doctorMap = doctors.stream()
                .collect(Collectors.toMap(Doctor::getId, doctor -> doctor));
            
            // 获取所有中医分类ID
            List<Long> categoryIds = doctors.stream()
                .map(Doctor::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

            // 批量查询中医分类信息
            Map<Long, Department> categoryMap = new HashMap<>();
            if (!categoryIds.isEmpty()) {
                List<Department> categories = departmentMapper.selectBatchIds(categoryIds);
                categoryMap = categories.stream()
                    .collect(Collectors.toMap(Department::getId, dept -> dept));
            }

            // 设置关联信息
            for (Schedule schedule : scheduleList) {
                Doctor doctor = doctorMap.get(schedule.getDoctorId());
                if (doctor != null) {
                    schedule.setDoctorName(doctor.getDoctorName());

                    Department category = categoryMap.get(doctor.getCategoryId());
                    if (category != null) {
                        schedule.setCategoryName(category.getCategoryName());
                        schedule.setDeptName(category.getCategoryName()); // 兼容字段
                    }
                }
            }
        }

        // 缓存结果（仅缓存前2页）
        if (page <= CacheConstants.ADMIN_SCHEDULE_LIST_HOT_PAGES) {
            redisUtil.set(cacheKey, schedules, CacheConstants.ADMIN_SCHEDULE_LIST_TTL_SECONDS, TimeUnit.SECONDS);
            log.info("已缓存排班列表: page={}, pageSize={}", page, pageSize);
        }
        
        return Result.success(schedules);
    }

    /**
     * 获取医生排班列表
     */
    @GetMapping("/doctor/{doctorId}")
    public Result<List<Schedule>> getDoctorSchedules(@PathVariable Long doctorId, @RequestParam Map<String, Object> params) {
        QueryWrapper<Schedule> wrapper = new QueryWrapper<>();
        wrapper.eq("doctor_id", doctorId);

        // 月份筛选
        if (params.containsKey("month") && StringUtils.hasText((String) params.get("month"))) {
            String month = (String) params.get("month");
            wrapper.likeRight("schedule_date", month);
        }

        wrapper.orderByAsc("schedule_date", "time_slot");
        List<Schedule> schedules = scheduleService.list(wrapper);
        return Result.success(schedules);
    }

    /**
     * 根据日期获取医生排班
     */
    @GetMapping("/doctor/{doctorId}/date/{date}")
    public Result<List<Schedule>> getDoctorScheduleByDate(@PathVariable Long doctorId, @PathVariable String date) {
        QueryWrapper<Schedule> wrapper = new QueryWrapper<>();
        wrapper.eq("doctor_id", doctorId)
               .eq("schedule_date", date)
               .orderByAsc("time_slot");
        List<Schedule> schedules = scheduleService.list(wrapper);
        return Result.success(schedules);
    }

    /**
     * 更新排班
     */
    @OperationLog(module = "SCHEDULE", type = "UPDATE", description = "更新排班")
    @PutMapping("/update")
    public Result<Boolean> updateSchedule(@RequestBody Schedule schedule) {
        boolean result = scheduleService.updateById(schedule);
        if (result) {
            // 失效管理员列表缓存
            redisUtil.deleteByPattern("hospital:admin:schedule:list:*");
            log.info("已失效排班列表缓存");
        }
        return Result.success(result);
    }

    /**
     * 删除排班
     */
    @OperationLog(module = "SCHEDULE", type = "DELETE", description = "删除排班")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteSchedule(@PathVariable Long id) {
        log.info("删除排班: id={}", id);
        boolean result = scheduleService.removeById(id);
        if (result) {
            // 失效管理员列表缓存
            redisUtil.deleteByPattern("hospital:admin:schedule:list:*");
            log.info("已失效排班列表缓存");
        }
        return Result.success(result);
    }

    /**
     * 获取排班详情
     */
    @GetMapping("/{id}")
    public Result<Schedule> getScheduleById(@PathVariable Long id) {
        Schedule schedule = scheduleService.getById(id);
        return Result.success(schedule);
    }

}
