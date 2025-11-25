package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.entity.Dictionary;
import com.hospital.entity.OperationLog;
import com.hospital.entity.SystemConfig;
import com.hospital.entity.User;
import java.util.List;
import java.util.Map;

/**
 * 系统管理服务接口
 */
public interface SystemService {
    
    // ==================== 操作日志管理 ====================
    
    /**
     * 获取操作日志列表
     * @param params 查询参数
     * @return 分页结果
     */
    IPage<OperationLog> getOperationLogs(Map<String, Object> params);
    
    /**
     * 记录操作日志
     * @param operationLog 操作日志
     */
    void recordOperationLog(OperationLog operationLog);

    /**
     * 导出操作日志
     * @param params 查询参数
     * @return Excel文件字节数组
     */
    byte[] exportOperationLogs(Map<String, Object> params);

    // ==================== 系统配置管理 ====================
    
    /**
     * 获取系统设置
     * @return 系统配置列表
     */
    List<SystemConfig> getSystemSettings();
    
    /**
     * 更新系统设置
     * @param configs 配置列表
     * @return 是否成功
     */
    boolean updateSystemSettings(List<SystemConfig> configs);
    
    /**
     * 根据键获取配置值
     * @param key 配置键
     * @return 配置值
     */
    String getConfigValue(String key);
    
    // ==================== 数据字典管理 ====================
    
    /**
     * 获取数据字典列表
     * @return 字典列表
     */
    List<Dictionary> getDictionaryList();

    /**
     * 根据类型获取数据字典列表
     * @param type 字典类型
     * @return 字典列表
     */
    List<Dictionary> getDictionaryListByType(String type);

    /**
     * 添加数据字典
     * @param dictionary 字典信息
     * @return 是否成功
     */
    boolean addDictionary(Dictionary dictionary);
    
    /**
     * 更新数据字典
     * @param dictionary 字典信息
     * @return 是否成功
     */
    boolean updateDictionary(Dictionary dictionary);
    
    /**
     * 删除数据字典
     * @param id 字典ID
     * @return 是否成功
     */
    boolean deleteDictionary(Long id);
    
    // ==================== 用户管理 ====================
    
    /**
     * 获取用户列表
     * @param params 查询参数
     * @return 分页结果
     */
    IPage<User> getUserList(Map<String, Object> params);
    
    /**
     * 添加用户
     * @param user 用户信息
     * @return 是否成功
     */
    boolean addUser(User user);
    
    /**
     * 更新用户
     * @param user 用户信息
     * @return 是否成功
     */
    boolean updateUser(User user);
    
    /**
     * 删除用户
     * @param id 用户ID
     * @return 是否成功
     */
    boolean deleteUser(Long id);
    
    /**
     * 重置用户密码
     * @param userId 用户ID
     * @return 新密码
     */
    String resetUserPassword(Long userId);
    
    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param status 状态（0-禁用 1-启用）
     * @return 是否成功
     */
    boolean updateUserStatus(Long userId, Integer status);

    /**
     * 管理员强制下线指定用户（删除Redis令牌与会话键）
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean forceLogout(Long userId);

    /**
     * 刷新用户列表缓存（管理员端）
     */
    void refreshUserListCache();
}
