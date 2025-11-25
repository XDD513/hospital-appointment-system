-- 为会话表添加单方面删除标记字段
-- 用于实现单方面删除功能，删除后只对当前用户隐藏，对方仍可查看

ALTER TABLE `conversation` 
ADD COLUMN `deleted_by_patient` tinyint NULL DEFAULT 0 COMMENT '患者是否删除 0-否 1-是' AFTER `status`,
ADD COLUMN `deleted_by_doctor` tinyint NULL DEFAULT 0 COMMENT '医生是否删除 0-否 1-是' AFTER `deleted_by_patient`;

-- 添加索引以提高查询性能
CREATE INDEX `idx_conversation_deleted_patient` ON `conversation`(`deleted_by_patient`);
CREATE INDEX `idx_conversation_deleted_doctor` ON `conversation`(`deleted_by_doctor`);

