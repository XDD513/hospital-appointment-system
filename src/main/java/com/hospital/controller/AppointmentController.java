package com.hospital.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.annotation.OperationLog;
import com.hospital.common.constant.AppointmentStatus;
import com.hospital.common.constant.SystemConstants;
import com.hospital.common.result.Result;
import com.hospital.entity.Appointment;
import com.hospital.entity.Doctor;
import com.hospital.entity.Department;
import com.hospital.entity.User;
import com.hospital.service.AppointmentService;
import com.hospital.service.ScheduleService;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.DepartmentMapper;
import com.hospital.mapper.UserMapper;
import com.hospital.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.hospital.util.RedisUtil;

/**
 * 预约管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private com.hospital.service.NotificationService notificationService;

    /**
     * 创建预约
     */
    @OperationLog(module = "APPOINTMENT", type = "INSERT", description = "创建预约")
    @PostMapping("/create")
    public Result<Appointment> createAppointment(@RequestBody Appointment appointment, HttpServletRequest request) {
        // 设置患者ID
        Long userId = jwtUtil.getUserIdFromRequest(request);
        appointment.setPatientId(userId);

        // 调用 Service 层的创建方法（会自动生成排队号）
        return appointmentService.createAppointment(appointment);
    }

    /**
     * 查询预约列表（管理员端）
     */
    @GetMapping("/list")
    public Result<IPage<Appointment>> getAppointmentList(@RequestParam Map<String, Object> params) {
        // 安全解析分页参数
        Integer page = 1;
        Integer pageSize = SystemConstants.DEFAULT_PAGE_SIZE;

        try {
            if (params.containsKey("page") && params.get("page") != null) {
                page = Integer.parseInt(params.get("page").toString());
            }
        } catch (NumberFormatException e) {
            log.warn("无效的页码参数：{}", params.get("page"));
        }

        try {
            if (params.containsKey("pageSize") && params.get("pageSize") != null) {
                pageSize = Integer.parseInt(params.get("pageSize").toString());
            }
        } catch (NumberFormatException e) {
            log.warn("无效的页面大小参数：{}", params.get("pageSize"));
        }

        QueryWrapper<Appointment> wrapper = new QueryWrapper<>();

        // 状态筛选
        if (params.containsKey("status") && StringUtils.hasText((String) params.get("status"))) {
            wrapper.eq("status", params.get("status"));
        }

        // 订单号筛选（支持模糊匹配）：前端显示的订单号即预约主键ID
        // 兼容参数名：orderNo 或 id（前端可能以 id 传递）
        String orderNoParam = params.get("orderNo") != null ? params.get("orderNo").toString().trim() : null;
        String idParam = params.get("id") != null ? params.get("id").toString().trim() : null;
        String orderKeyword = StringUtils.hasText(orderNoParam) ? orderNoParam : (StringUtils.hasText(idParam) ? idParam : null);
        if (StringUtils.hasText(orderKeyword)) {
            // 仅当纯数字时进行按ID模糊匹配
            if (orderKeyword.matches("\\d+")) {
                wrapper.like("id", orderKeyword);
            }
        }

        // 患者姓名筛选
        if (params.containsKey("patientName") && StringUtils.hasText((String) params.get("patientName"))) {
            wrapper.like("patient_name", params.get("patientName"));
        }

        // 日期范围筛选
        if (params.containsKey("startDate") && StringUtils.hasText((String) params.get("startDate"))) {
            wrapper.ge("appointment_date", params.get("startDate"));
        }
        if (params.containsKey("endDate") && StringUtils.hasText((String) params.get("endDate"))) {
            wrapper.le("appointment_date", params.get("endDate"));
        }

        wrapper.orderByDesc("created_at");

        // 构造缓存键（仅缓存前2页）
        Map<String, Object> filterParams = new java.util.HashMap<>();
        if (params.containsKey("status") && StringUtils.hasText((String) params.get("status"))) {
            filterParams.put("status", params.get("status"));
        }
        if (params.containsKey("id") && StringUtils.hasText((String) params.get("id"))) {
            filterParams.put("id", params.get("id"));
        }
        if (params.containsKey("patientName") && StringUtils.hasText((String) params.get("patientName"))) {
            filterParams.put("patientName", params.get("patientName"));
        }
        if (params.containsKey("startDate") && StringUtils.hasText((String) params.get("startDate"))) {
            filterParams.put("startDate", params.get("startDate"));
        }
        if (params.containsKey("endDate") && StringUtils.hasText((String) params.get("endDate"))) {
            filterParams.put("endDate", params.get("endDate"));
        }

        String cacheKey = redisUtil.buildCacheKey("hospital:admin:appointment:list", page, pageSize, filterParams);

        // 管理员列表缓存TTL（5分钟）
        long ADMIN_APPOINTMENT_LIST_TTL_SECONDS = 300;
        int ADMIN_APPOINTMENT_LIST_HOT_PAGES = 2;

        // 仅缓存前2页
        if (page <= ADMIN_APPOINTMENT_LIST_HOT_PAGES) {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                IPage<Appointment> cachedPage = (IPage<Appointment>) cached;
                log.info("从缓存获取预约列表: page={}, pageSize={}", page, pageSize);
                return Result.success(cachedPage);
            }
        }

        Page<Appointment> pageObject = new Page<>(page, pageSize);
        IPage<Appointment> appointments = appointmentService.page(pageObject, wrapper);

        // 使用Service层方法丰富预约信息（关联查询医生、科室、患者信息）
        ((com.hospital.service.impl.AppointmentServiceImpl) appointmentService).enrichAppointmentPage(appointments);

        // 缓存结果（仅缓存前2页）
        if (page <= ADMIN_APPOINTMENT_LIST_HOT_PAGES) {
            redisUtil.set(cacheKey, appointments, ADMIN_APPOINTMENT_LIST_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            log.info("已缓存预约列表: page={}, pageSize={}", page, pageSize);
        }

        return Result.success(appointments);
    }

    /**
     * 查询患者预约列表
     */
    @GetMapping("/patient/list")
    public Result<List<Appointment>> getPatientAppointments(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);

        QueryWrapper<Appointment> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .orderByDesc("created_at");

        List<Appointment> appointments = appointmentService.list(wrapper);

        // 使用Service层方法丰富预约信息（关联查询医生、科室、患者信息）
        ((com.hospital.service.impl.AppointmentServiceImpl) appointmentService).enrichAppointmentList(appointments);

        return Result.success(appointments);
    }

    /**
     * 导出患者预约记录（Excel）
     */
    @GetMapping("/patient/export")
    public void exportPatientAppointments(@RequestParam Map<String, Object> params,
                                          HttpServletRequest request,
                                          HttpServletResponse response) throws java.io.IOException {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        byte[] data = appointmentService.exportPatientAppointments(userId, params);

        String fileName = URLEncoder.encode("预约记录_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename*=utf-8''" + fileName);
        response.getOutputStream().write(data);
    }

    /**
     * 取消预约
     */
    @OperationLog(module = "APPOINTMENT", type = "UPDATE", description = "取消预约")
    @PostMapping("/cancel/{id}")
    public Result<Boolean> cancelAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.getById(id);
        if (appointment != null) {
            // 增加号源（释放号源）
            if (appointment.getScheduleId() != null) {
                try {
                    scheduleService.increaseQuota(appointment.getScheduleId());
                    log.info("已释放号源，scheduleId={}", appointment.getScheduleId());
                } catch (Exception e) {
                    log.warn("释放号源失败: scheduleId={}, error={}", appointment.getScheduleId(), e.getMessage());
                }
            }
            
            // 更新状态为已取消
            appointment.setStatus(AppointmentStatus.CANCELLED.getCode());
            boolean result = appointmentService.updateById(appointment);
            if (result) {
                // 失效管理员列表缓存
                redisUtil.deleteByPattern("hospital:admin:appointment:list:*");
                log.info("已失效预约列表缓存");
                
                // 失效医生端相关缓存（无论预约日期是否为今日，都需要刷新）
                try {
                    Long doctorId = appointment.getDoctorId();
                    if (doctorId != null) {
                        // 清理医生端患者列表缓存
                        redisUtil.delete("hospital:doctor:patient:list:pending:doctor:" + doctorId);
                        redisUtil.delete("hospital:doctor:patient:list:today:doctor:" + doctorId);
                        redisUtil.delete("hospital:doctor:patient:list:completed:doctor:" + doctorId);
                        log.info("已失效医生端患者列表缓存，doctorId={}", doctorId);
                        
                        // 清理医生端今日统计缓存（如果预约日期是今日）
                        if (appointment.getAppointmentDate() != null && 
                            appointment.getAppointmentDate().equals(LocalDate.now())) {
                            String todayKey = "hospital:doctor:stats:today:doctor:" + doctorId + ":date:" + LocalDate.now();
                            redisUtil.delete(todayKey);
                            log.info("已失效医生端今日统计缓存，doctorId={}", doctorId);
                        }
                    }
                    
                    // 失效患者最近预约缓存
                    if (appointment.getPatientId() != null) {
                        redisUtil.delete("hospital:patient:stats:appointments:recent:patient:" + appointment.getPatientId());
                        log.info("已失效患者最近预约缓存，patientId={}", appointment.getPatientId());
                    }
                } catch (Exception e) {
                    log.warn("取消预约后失效缓存失败: appointmentId={}, error={}", id, e.getMessage());
                }
                
                // 发送通知给医生：取消预约
                try {
                    com.hospital.entity.Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
                    if (doctor != null && doctor.getUserId() != null) {
                        String patientName = appointment.getPatientName() != null ? appointment.getPatientName() : "患者";
                        String appointmentDateStr = appointment.getAppointmentDate() != null ? 
                                appointment.getAppointmentDate().toString() : "";
                        String content = String.format("患者%s已取消%s的预约", patientName, appointmentDateStr);
                        notificationService.createAndSendNotification(
                                doctor.getUserId(),
                                "预约取消通知",
                                content,
                                "APPOINTMENT_CANCELLED"
                        );
                    }
                } catch (Exception e) {
                    log.warn("发送取消预约通知失败: appointmentId={}, error={}", id, e.getMessage());
                }
            }
            return Result.success(result);
        }
        return Result.error("预约不存在");
    }

    /**
     * 获取预约详情
     */
    @GetMapping("/{id}")
    public Result<Appointment> getAppointmentById(@PathVariable Long id) {
        Appointment appointment = appointmentService.getById(id);
        return Result.success(appointment);
    }

    /**
     * 更新预约状态
     */
    @OperationLog(module = "APPOINTMENT", type = "UPDATE", description = "更新预约状态")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateAppointmentStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String status = request.get("status");

        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setStatus(status);

        boolean result = appointmentService.updateById(appointment);

        // 若更新成功，且为今日预约，失效医生端今日患者与统计缓存；同时失效患者最近预约缓存
        if (result) {
            try {
                Appointment updated = appointmentService.getById(id);
                if (updated != null) {
                    // 失效管理员列表缓存
                    redisUtil.deleteByPattern("hospital:admin:appointment:list:*");
                    log.info("已失效预约列表缓存");

                    // 失效患者最近预约缓存（任意日期都需要刷新）
                    if (updated.getPatientId() != null) {
                        redisUtil.deleteByPattern("stats:patient:" + updated.getPatientId() + ":recentAppointments:*");
                    }

                    // 仅当今日预约时，失效医生端今日缓存
                    if (updated.getAppointmentDate() != null && updated.getAppointmentDate().equals(LocalDate.now())) {
                        Long doctorId = updated.getDoctorId();
                        if (doctorId != null) {
                            redisUtil.delete("patient:doctor:" + doctorId + ":pending");
                            redisUtil.delete("patient:doctor:" + doctorId + ":today");
                            redisUtil.delete("patient:doctor:" + doctorId + ":completed");
                            String todayKey = "stats:doctor:" + doctorId + ":today:" + LocalDate.now();
                            redisUtil.delete(todayKey);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return Result.success(result);
    }

    /**
     * 删除预约
     */
    @OperationLog(module = "APPOINTMENT", type = "DELETE", description = "删除预约")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteAppointment(@PathVariable Long id) {
        boolean result = appointmentService.removeById(id);
        if (result) {
            // 失效管理员列表缓存
            redisUtil.deleteByPattern("hospital:admin:appointment:list:*");
            log.info("已失效预约列表缓存");
        }
        return Result.success(result);
    }

}
