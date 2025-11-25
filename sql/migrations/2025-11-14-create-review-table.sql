-- ----------------------------
-- Table structure for review
-- ----------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
  `id` bigint NOT NULL COMMENT '评价ID',
  `appointment_id` bigint NULL DEFAULT NULL COMMENT '预约ID',
  `consultation_record_id` bigint NULL DEFAULT NULL COMMENT '接诊记录ID',
  `patient_id` bigint NOT NULL COMMENT '患者ID',
  `doctor_id` bigint NOT NULL COMMENT '医生ID',
  `category_id` bigint NULL DEFAULT NULL COMMENT '分类ID（中医分类）',
  `rating` int NOT NULL COMMENT '评分（1-5分）',
  `service_rating` int NULL DEFAULT NULL COMMENT '服务态度评分（1-5分）',
  `professional_rating` int NULL DEFAULT NULL COMMENT '专业水平评分（1-5分）',
  `environment_rating` int NULL DEFAULT NULL COMMENT '环境评分（1-5分）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '评价内容',
  `is_anonymous` int NULL DEFAULT 0 COMMENT '是否匿名 0-否 1-是',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '标签（JSON格式）',
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '图片（JSON格式）',
  `doctor_reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '医生回复',
  `doctor_reply_time` datetime NULL DEFAULT NULL COMMENT '医生回复时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PENDING' COMMENT '状态 PENDING-待发布 PUBLISHED-已发布 HIDDEN-已隐藏',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_appointment_id`(`appointment_id` ASC) USING BTREE,
  INDEX `idx_patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `idx_doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评价表' ROW_FORMAT = Dynamic;

