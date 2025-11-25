-- ===================================================================
-- 创建接诊记录表 consultation_record
-- 创建时间: 2025-11-08
-- 说明: 用于存储医生接诊记录的详细信息
-- ===================================================================

-- 删除已存在的表（如果存在）
DROP TABLE IF EXISTS `consultation_record`;

-- 创建接诊记录表
CREATE TABLE `consultation_record` (
    `id` BIGINT NOT NULL COMMENT '接诊记录ID',
    `appointment_id` BIGINT NOT NULL COMMENT '预约ID',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
    `category_id` BIGINT NOT NULL COMMENT '分类ID（中医分类）',
    `consultation_date` DATE NOT NULL COMMENT '接诊日期',
    `consultation_time` TIME NULL DEFAULT NULL COMMENT '接诊时间',
    `chief_complaint` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '主诉',
    `present_illness` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '现病史',
    `past_history` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '既往史',
    `physical_examination` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '体格检查',
    `auxiliary_examination` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '辅助检查',
    `diagnosis` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '诊断',
    `treatment_plan` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '治疗方案',
    `prescription` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '处方（JSON格式）',
    `follow_up_advice` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '随访建议',
    `consultation_fee` DECIMAL(10, 2) NULL DEFAULT 0.00 COMMENT '诊疗费',
    `duration_minutes` INT NULL DEFAULT 0 COMMENT '接诊时长（分钟）',
    `status` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态 IN_PROGRESS-进行中 COMPLETED-已完成',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_appointment_id`(`appointment_id` ASC) USING BTREE COMMENT '预约ID唯一索引',
    INDEX `idx_patient_id`(`patient_id` ASC) USING BTREE COMMENT '患者ID索引',
    INDEX `idx_doctor_id`(`doctor_id` ASC) USING BTREE COMMENT '医生ID索引',
    INDEX `idx_category_id`(`category_id` ASC) USING BTREE COMMENT '分类ID索引',
    INDEX `idx_consultation_date`(`consultation_date` ASC) USING BTREE COMMENT '接诊日期索引',
    INDEX `idx_status`(`status` ASC) USING BTREE COMMENT '状态索引',
    INDEX `idx_doctor_status`(`doctor_id` ASC, `status` ASC) USING BTREE COMMENT '医生ID和状态组合索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '接诊记录表' ROW_FORMAT = Dynamic;

-- 添加注释说明
ALTER TABLE `consultation_record` COMMENT = '接诊记录表：存储医生接诊患者的详细记录，包括主诉、诊断、治疗方案等信息';

