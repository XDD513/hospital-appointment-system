package com.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.annotation.OperationLog;
import com.hospital.common.result.Result;
import com.hospital.entity.Dictionary;
import com.hospital.entity.SystemConfig;
import com.hospital.entity.User;
import com.hospital.service.SystemService;
import com.hospital.util.JwtUtil;
import com.hospital.dto.request.SystemSettingsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private SystemService systemService;

    @Autowired
    private JwtUtil jwtUtil;

    // ==================== 操作日志管理 ====================

    /**
     * 获取操作日志列表
     */
    @GetMapping("/logs")
    public Result<IPage<com.hospital.entity.OperationLog>> getOperationLogs(@RequestParam Map<String, Object> params) {
        IPage<com.hospital.entity.OperationLog> logs = systemService.getOperationLogs(params);
        return Result.success(logs);
    }

    /**
     * 导出操作日志
     */
    @OperationLog(module = "SYSTEM", type = "SELECT", description = "导出操作日志")
    @GetMapping("/logs/export")
    public void exportOperationLogs(@RequestParam Map<String, Object> params, HttpServletResponse response) throws IOException {
        log.info("导出操作日志，参数：{}", params);

        byte[] data = systemService.exportOperationLogs(params);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=operation_logs.xlsx");
        response.getOutputStream().write(data);
    }

    // ==================== 系统配置管理 ====================

    /**
     * 获取系统设置
     */
    @GetMapping("/settings")
    public Result<Map<String, Map<String, Object>>> getSystemSettings() {
        List<SystemConfig> settings = systemService.getSystemSettings();
        
        // 将配置列表转换为前端需要的分组格式
        Map<String, Map<String, Object>> groupedSettings = new HashMap<>();
        
        for (SystemConfig config : settings) {
            String configKey = config.getConfigKey();
            if (configKey == null) {
                continue;
            }
            
            // 根据配置键确定前端分组
            String frontendGroup = determineFrontendGroup(configKey);
            if (frontendGroup == null) {
                continue;
            }
            
            // 确保分组存在
            groupedSettings.putIfAbsent(frontendGroup, new HashMap<>());
            
            // 将数据库配置键转换为前端驼峰命名
            String frontendKey = convertToFrontendKey(configKey, frontendGroup);
            Object value = convertConfigValue(config.getConfigValue(), config.getConfigType());
            
            groupedSettings.get(frontendGroup).put(frontendKey, value);
        }
        
        return Result.success(groupedSettings);
    }
    
    /**
     * 根据配置键确定前端分组
     */
    private String determineFrontendGroup(String configKey) {
        if (configKey.startsWith("system.") || configKey.startsWith("appointment.") || configKey.startsWith("payment.")) {
            return "basic";
        } else if (configKey.startsWith("notification.")) {
            return "notification";
        } else if (configKey.startsWith("security.")) {
            return "security";
        } else if (configKey.startsWith("email.")) {
            return "email";
        }
        return null;
    }
    
    /**
     * 将数据库配置键转换为前端驼峰命名
     */
    private String convertToFrontendKey(String configKey, String group) {
        // 基础设置映射
        if ("basic".equals(group)) {
            switch (configKey) {
                case "system.name": return "systemName";
                case "system.version": return "systemVersion";
                case "appointment.advance_days": return "advanceDays";
                case "appointment.cancel_hours": return "cancelHours";
                case "payment.timeout": return "paymentTimeout";
                case "system.maintenance_mode": return "maintenanceMode";
            }
        }
        // 通知设置映射
        else if ("notification".equals(group)) {
            switch (configKey) {
                case "notification.appointment_reminder": return "appointmentReminder";
                case "notification.reminder_hours": return "reminderHours";
                case "notification.sms_enabled": return "smsNotification";
                case "notification.email_enabled": return "emailNotification";
                case "notification.system_enabled": return "systemNotification";
            }
        }
        // 安全设置映射
        else if ("security".equals(group)) {
            switch (configKey) {
                case "security.min_password_length": return "minPasswordLength";
                case "security.login_lock_enabled": return "loginLockEnabled";
                case "security.max_login_attempts": return "maxLoginAttempts";
                case "security.lock_duration": return "lockDuration";
                case "security.session_timeout": return "sessionTimeout";
            }
        }
        // 邮件设置映射
        else if ("email".equals(group)) {
            switch (configKey) {
                case "email.smtp_host": return "smtpHost";
                case "email.smtp_port": return "smtpPort";
                case "email.from_email": return "fromEmail";
                case "email.password": return "emailPassword";
                case "email.ssl_enabled": return "sslEnabled";
            }
        }
        
        // 如果没有匹配的映射，返回原键名
        return configKey;
    }
    
    /**
     * 转换配置值类型
     */
    private Object convertConfigValue(String value, String type) {
        if (value == null) {
            return null;
        }
        
        // 尝试转换为合适的类型
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        
        try {
            // 尝试转换为数字
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // 不是数字，返回字符串
            return value;
        }
    }

    /**
     * 更新系统设置
     */
    @PutMapping("/settings")
    public Result<Boolean> updateSystemSettings(@RequestBody SystemSettingsRequest request) {
        log.info("更新系统设置，类型：{}", request.getType());
        
        // 将前端数据转换为SystemConfig列表
        List<SystemConfig> configs = convertToSystemConfigs(request);
        
        boolean result = systemService.updateSystemSettings(configs);
        return Result.success(result);
    }
    
    /**
     * 将前端请求转换为SystemConfig列表
     */
    private List<SystemConfig> convertToSystemConfigs(SystemSettingsRequest request) {
        List<SystemConfig> configs = new ArrayList<>();
        Map<String, Object> data = request.getData();
        
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                SystemConfig config = new SystemConfig();
                // 将驼峰命名转换为数据库中的配置键格式
                String configKey = convertToConfigKey(entry.getKey(), request.getType());
                config.setConfigKey(configKey);
                config.setConfigValue(String.valueOf(entry.getValue()));
                config.setConfigType(request.getType());
                config.setConfigGroup(request.getType());
                configs.add(config);
            }
        }
        
        return configs;
    }
    
    /**
     * 将前端驼峰命名转换为数据库配置键格式
     */
    private String convertToConfigKey(String camelCaseKey, String type) {
        // 基础设置映射
        if ("basic".equals(type)) {
            switch (camelCaseKey) {
                case "systemName": return "system.name";
                case "systemVersion": return "system.version";
                case "advanceDays": return "appointment.advance_days";
                case "cancelHours": return "appointment.cancel_hours";
                case "paymentTimeout": return "payment.timeout";
                case "maintenanceMode": return "system.maintenance_mode";
            }
        }
        // 通知设置映射
        else if ("notification".equals(type)) {
            switch (camelCaseKey) {
                case "appointmentReminder": return "notification.appointment_reminder";
                case "reminderHours": return "notification.reminder_hours";
                case "smsNotification": return "notification.sms_enabled";
                case "emailNotification": return "notification.email_enabled";
                case "systemNotification": return "notification.system_enabled";
            }
        }
        // 安全设置映射
        else if ("security".equals(type)) {
            switch (camelCaseKey) {
                case "minPasswordLength": return "security.min_password_length";
                case "loginLockEnabled": return "security.login_lock_enabled";
                case "maxLoginAttempts": return "security.max_login_attempts";
                case "lockDuration": return "security.lock_duration";
                case "sessionTimeout": return "security.session_timeout";
            }
        }
        // 邮件设置映射
        else if ("email".equals(type)) {
            switch (camelCaseKey) {
                case "smtpHost": return "email.smtp_host";
                case "smtpPort": return "email.smtp_port";
                case "fromEmail": return "email.from_email";
                case "emailPassword": return "email.password";
                case "sslEnabled": return "email.ssl_enabled";
            }
        }
        
        // 如果没有匹配的映射，返回原键名
        return camelCaseKey;
    }

    // ==================== 数据字典管理 ====================

    /**
     * 获取数据字典列表
     */
    @GetMapping("/dictionary")
    public Result<List<Dictionary>> getDictionaryList(@RequestParam(required = false) String type) {
        List<Dictionary> dictionaries;
        if (type != null && !type.isEmpty()) {
            dictionaries = systemService.getDictionaryListByType(type);
        } else {
            dictionaries = systemService.getDictionaryList();
        }
        return Result.success(dictionaries);
    }

    /**
     * 添加数据字典
     */
    @OperationLog(module = "SYSTEM", type = "INSERT", description = "添加数据字典")
    @PostMapping("/dictionary")
    public Result<Boolean> addDictionary(@RequestBody Dictionary dictionary) {
        boolean result = systemService.addDictionary(dictionary);
        return Result.success(result);
    }

    /**
     * 更新数据字典
     */
    @OperationLog(module = "SYSTEM", type = "UPDATE", description = "更新数据字典")
    @PutMapping("/dictionary")
    public Result<Boolean> updateDictionary(@RequestBody Dictionary dictionary) {
        boolean result = systemService.updateDictionary(dictionary);
        return Result.success(result);
    }

    /**
     * 删除数据字典
     */
    @OperationLog(module = "SYSTEM", type = "DELETE", description = "删除数据字典")
    @DeleteMapping("/dictionary/{id}")
    public Result<Boolean> deleteDictionary(@PathVariable Long id) {
        boolean result = systemService.deleteDictionary(id);
        return Result.success(result);
    }

    // ==================== 用户管理 ====================

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public Result<IPage<User>> getUserList(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }
        
        IPage<User> users = systemService.getUserList(params);
        return Result.success(users);
    }

    /**
     * 添加用户
     */
    @OperationLog(module = "USER", type = "INSERT", description = "添加用户")
    @PostMapping("/user")
    public Result<Boolean> addUser(@RequestBody User user, HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }

        boolean result = systemService.addUser(user);
        return Result.success(result);
    }

    /**
     * 更新用户
     */
    @OperationLog(module = "USER", type = "UPDATE", description = "更新用户信息")
    @PutMapping("/user")
    public Result<Boolean> updateUser(@RequestBody User user, HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }

        boolean result = systemService.updateUser(user);
        return Result.success(result);
    }

    /**
     * 删除用户
     */
    @OperationLog(module = "USER", type = "DELETE", description = "删除用户")
    @DeleteMapping("/user/{id}")
    public Result<Boolean> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }

        boolean result = systemService.deleteUser(id);
        return Result.success(result);
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/user/{userId}/reset-password")
    public Result<Map<String, String>> resetUserPassword(@PathVariable Long userId, HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }
        
        String newPassword = systemService.resetUserPassword(userId);

        Map<String, String> result = new HashMap<>();
        result.put("newPassword", newPassword);

        return Result.success(result);
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/user/{userId}/status")
    public Result<Boolean> updateUserStatus(@PathVariable Long userId, @RequestBody Map<String, Integer> request, HttpServletRequest httpRequest) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(httpRequest);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }
        
        Integer status = request.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "状态参数无效");
        }
        
        boolean result = systemService.updateUserStatus(userId, status);
        return Result.success(result);
    }

    /**
     * 管理员强制下线：删除指定用户在Redis中的令牌与会话
     */
    @PostMapping("/user/{userId}/force-logout")
    public Result<Boolean> forceLogout(@PathVariable Long userId, HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }

        boolean result = systemService.forceLogout(userId);
        return Result.success(result);
    }

}
