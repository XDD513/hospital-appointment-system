-- 添加ADMIN到sender_role枚举类型
-- 用于支持管理员对话功能

-- 修改conversation_message表的sender_role枚举，添加ADMIN选项
ALTER TABLE `conversation_message` 
MODIFY COLUMN `sender_role` enum('PATIENT','DOCTOR','SYSTEM','ADMIN') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发送者角色';

-- 修改conversation表的last_sender_role枚举，添加ADMIN选项
ALTER TABLE `conversation` 
MODIFY COLUMN `last_sender_role` enum('PATIENT','DOCTOR','SYSTEM','ADMIN') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后发送者角色';

