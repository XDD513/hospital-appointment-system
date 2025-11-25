-- 重构conversation表以支持三种身份（患者、医生、管理员）
-- Date: 2025-11-15

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 添加新字段
ALTER TABLE `conversation`
    ADD COLUMN `conversation_type` enum('PATIENT_DOCTOR','ADMIN_USER') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PATIENT_DOCTOR' COMMENT '对话类型：PATIENT_DOCTOR-患者医生对话，ADMIN_USER-管理员用户对话' AFTER `doctor_id`,
    ADD COLUMN `participant1_user_id` bigint NULL COMMENT '参与者1的用户ID' AFTER `conversation_type`,
    ADD COLUMN `participant1_role` enum('PATIENT','DOCTOR','ADMIN') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '参与者1的角色' AFTER `participant1_user_id`,
    ADD COLUMN `participant2_user_id` bigint NULL COMMENT '参与者2的用户ID' AFTER `participant1_role`,
    ADD COLUMN `participant2_role` enum('PATIENT','DOCTOR','ADMIN') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '参与者2的角色' AFTER `participant2_user_id`,
    ADD COLUMN `unread_for_participant1` int NULL DEFAULT 0 COMMENT '参与者1未读数量' AFTER `unread_for_doctor`,
    ADD COLUMN `unread_for_participant2` int NULL DEFAULT 0 COMMENT '参与者2未读数量' AFTER `unread_for_participant1`,
    ADD COLUMN `deleted_by_participant1` tinyint NULL DEFAULT 0 COMMENT '参与者1是否删除 0-否 1-是' AFTER `deleted_by_doctor`,
    ADD COLUMN `deleted_by_participant2` tinyint NULL DEFAULT 0 COMMENT '参与者2是否删除 0-否 1-是' AFTER `deleted_by_participant1`;

-- 添加索引
ALTER TABLE `conversation`
    ADD INDEX `idx_conversation_type`(`conversation_type` ASC) USING BTREE,
    ADD INDEX `idx_participant1_user`(`participant1_user_id` ASC) USING BTREE,
    ADD INDEX `idx_participant2_user`(`participant2_user_id` ASC) USING BTREE,
    ADD INDEX `idx_participant1_role`(`participant1_role` ASC) USING BTREE,
    ADD INDEX `idx_participant2_role`(`participant2_role` ASC) USING BTREE;

-- 迁移现有数据
-- 1. 迁移普通患者-医生对话（doctor_id在tcm_doctor表中存在）
UPDATE `conversation` c
INNER JOIN `tcm_doctor` d ON c.`doctor_id` = d.`id`
SET 
    c.`conversation_type` = 'PATIENT_DOCTOR',
    c.`participant1_user_id` = c.`patient_id`,
    c.`participant1_role` = 'PATIENT',
    c.`participant2_user_id` = d.`user_id`,
    c.`participant2_role` = 'DOCTOR',
    c.`unread_for_participant1` = c.`unread_for_patient`,
    c.`unread_for_participant2` = c.`unread_for_doctor`,
    c.`deleted_by_participant1` = c.`deleted_by_patient`,
    c.`deleted_by_participant2` = c.`deleted_by_doctor`
WHERE d.`id` IS NOT NULL;

-- 2. 迁移管理员对话（doctor_id不在tcm_doctor表中，但在user表中且role_type=3）
UPDATE `conversation` c
LEFT JOIN `tcm_doctor` d ON c.`doctor_id` = d.`id`
INNER JOIN `user` u ON c.`doctor_id` = u.`id` AND u.`role_type` = 3
LEFT JOIN `user` u2 ON c.`patient_id` = u2.`id`
SET 
    c.`conversation_type` = 'ADMIN_USER',
    c.`participant1_user_id` = c.`patient_id`,
    c.`participant1_role` = CASE 
        WHEN u2.`role_type` = 1 THEN 'PATIENT'
        WHEN u2.`role_type` = 2 THEN 'DOCTOR'
        ELSE 'PATIENT'
    END,
    c.`participant2_user_id` = c.`doctor_id`,
    c.`participant2_role` = 'ADMIN',
    c.`unread_for_participant1` = c.`unread_for_patient`,
    c.`unread_for_participant2` = c.`unread_for_doctor`,
    c.`deleted_by_participant1` = c.`deleted_by_patient`,
    c.`deleted_by_participant2` = c.`deleted_by_doctor`
WHERE d.`id` IS NULL;

SET FOREIGN_KEY_CHECKS = 1;

