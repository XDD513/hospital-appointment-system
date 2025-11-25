-- 添加预约ID字段到体质测试表
-- 用于关联预约与体质测试，让医生看到患者在本次预约期间完成的测试

USE tcm_health_system;

-- 添加 appointment_id 字段
ALTER TABLE `user_constitution_test` 
ADD COLUMN `appointment_id` BIGINT NULL COMMENT '关联的预约ID（如果是通过预约提醒进行的测试）' AFTER `user_id`;

-- 添加索引以提高查询性能
ALTER TABLE `user_constitution_test` 
ADD INDEX `idx_appointment_id` (`appointment_id` ASC);

-- 添加外键约束（可选，根据需要决定是否启用）
-- ALTER TABLE `user_constitution_test` 
-- ADD CONSTRAINT `fk_test_appointment` 
-- FOREIGN KEY (`appointment_id`) REFERENCES `appointment` (`id`) 
-- ON DELETE SET NULL ON UPDATE CASCADE;

