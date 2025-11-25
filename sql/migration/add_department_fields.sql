-- =============================================
-- 数据库迁移脚本：为 tcm_category 表添加科室管理字段
-- 创建时间：2025-11-09
-- 说明：添加负责人、联系电话、位置字段
-- =============================================

USE `tcm_health_system`;

-- 添加负责人字段
ALTER TABLE `tcm_category` 
ADD COLUMN `dept_head` VARCHAR(50) NULL COMMENT '负责人姓名' AFTER `category_desc`;

-- 添加联系电话字段
ALTER TABLE `tcm_category` 
ADD COLUMN `contact_phone` VARCHAR(20) NULL COMMENT '联系电话' AFTER `dept_head`;

-- 添加位置字段
ALTER TABLE `tcm_category` 
ADD COLUMN `location` VARCHAR(100) NULL COMMENT '科室位置' AFTER `contact_phone`;

-- 查看表结构确认
DESC `tcm_category`;

-- 显示修改结果
SELECT '数据库迁移完成：已为 tcm_category 表添加 dept_head, contact_phone, location 字段' AS message;

