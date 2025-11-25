-- ============================================
-- 医院预约挂号系统 - 数据库建表脚本
-- 数据库: hospital_appointment_system
-- 版本: 1.0.0
-- 日期: 2025-10-24
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS hospital_appointment_system 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE hospital_appointment_system;

-- ============================================
-- 1. 用户表 (tb_user)
-- ============================================
CREATE TABLE tb_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    id_card VARCHAR(18) COMMENT '身份证号',
    phone VARCHAR(11) NOT NULL COMMENT '手机号',
    gender TINYINT DEFAULT 0 COMMENT '性别（0-未知 1-男 2-女）',
    birth_date DATE COMMENT '出生日期',
    avatar VARCHAR(255) COMMENT '头像URL',
    role_type VARCHAR(20) NOT NULL DEFAULT 'PATIENT' COMMENT '角色类型（PATIENT-患者 DOCTOR-医生 ADMIN-管理员）',
    status TINYINT DEFAULT 1 COMMENT '状态（0-禁用 1-启用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_phone (phone),
    INDEX idx_id_card (id_card),
    INDEX idx_role_type (role_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 科室表 (tb_department)
-- ============================================
CREATE TABLE tb_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '科室ID',
    dept_name VARCHAR(100) NOT NULL COMMENT '科室名称',
    dept_code VARCHAR(50) NOT NULL UNIQUE COMMENT '科室代码',
    dept_type VARCHAR(50) COMMENT '科室类型（内科、外科、儿科等）',
    description TEXT COMMENT '科室简介',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态（0-停用 1-启用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_dept_code (dept_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室表';

-- ============================================
-- 3. 医生表 (tb_doctor)
-- ============================================
CREATE TABLE tb_doctor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '医生ID',
    user_id BIGINT NOT NULL COMMENT '用户ID（关联tb_user）',
    dept_id BIGINT NOT NULL COMMENT '科室ID（关联tb_department）',
    title VARCHAR(50) COMMENT '职称（主任医师、副主任医师、主治医师等）',
    specialty VARCHAR(255) COMMENT '专长',
    introduction TEXT COMMENT '医生简介',
    consultation_fee DECIMAL(10,2) DEFAULT 0.00 COMMENT '挂号费',
    rating DECIMAL(3,2) DEFAULT 0.00 COMMENT '评分（0-5分）',
    consultation_count INT DEFAULT 0 COMMENT '接诊量',
    status TINYINT DEFAULT 1 COMMENT '状态（0-离职 1-在职 2-休假）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    FOREIGN KEY (dept_id) REFERENCES tb_department(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生表';

-- ============================================
-- 4. 出诊排班表 (tb_schedule)
-- ============================================
CREATE TABLE tb_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '排班ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID（关联tb_doctor）',
    schedule_date DATE NOT NULL COMMENT '出诊日期',
    time_slot VARCHAR(20) NOT NULL COMMENT '时段（MORNING-上午 AFTERNOON-下午 EVENING-晚间）',
    total_quota INT NOT NULL DEFAULT 0 COMMENT '总号源数',
    remaining_quota INT NOT NULL DEFAULT 0 COMMENT '剩余号源数',
    status TINYINT DEFAULT 1 COMMENT '状态（0-停诊 1-可预约 2-已满）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (doctor_id) REFERENCES tb_doctor(id) ON DELETE CASCADE,
    UNIQUE KEY uk_doctor_date_slot (doctor_id, schedule_date, time_slot),
    INDEX idx_schedule_date (schedule_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出诊排班表';

-- ============================================
-- 5. 预约挂号表 (tb_appointment)
-- ============================================
CREATE TABLE tb_appointment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预约ID',
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    patient_id BIGINT NOT NULL COMMENT '患者ID（关联tb_user）',
    doctor_id BIGINT NOT NULL COMMENT '医生ID（关联tb_doctor）',
    dept_id BIGINT NOT NULL COMMENT '科室ID（关联tb_department）',
    schedule_id BIGINT NOT NULL COMMENT '排班ID（关联tb_schedule）',
    appointment_date DATE NOT NULL COMMENT '预约日期',
    time_slot VARCHAR(20) NOT NULL COMMENT '时段',
    queue_number INT COMMENT '挂号序号',
    consultation_fee DECIMAL(10,2) NOT NULL COMMENT '挂号费',
    appointment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '预约状态（PENDING_PAYMENT-待支付 PENDING_VISIT-待就诊 COMPLETED-已完成 CANCELLED-已取消 NO_SHOW-爽约）',
    payment_status VARCHAR(20) DEFAULT 'UNPAID' COMMENT '支付状态（UNPAID-未支付 PAID-已支付 REFUNDED-已退款）',
    cancel_reason VARCHAR(255) COMMENT '取消原因',
    remark TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (patient_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES tb_doctor(id) ON DELETE CASCADE,
    FOREIGN KEY (dept_id) REFERENCES tb_department(id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_id) REFERENCES tb_schedule(id) ON DELETE CASCADE,
    INDEX idx_order_no (order_no),
    INDEX idx_patient_id (patient_id),
    INDEX idx_doctor_id (doctor_id),
    INDEX idx_appointment_date (appointment_date),
    INDEX idx_status (appointment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约挂号表';

-- ============================================
-- 6. 支付记录表 (tb_payment)
-- ============================================
CREATE TABLE tb_payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支付ID',
    appointment_id BIGINT NOT NULL COMMENT '预约ID（关联tb_appointment）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    payment_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    payment_method VARCHAR(20) COMMENT '支付方式（ALIPAY-支付宝 WECHAT-微信 CASH-现金）',
    transaction_no VARCHAR(100) COMMENT '支付流水号',
    payment_status VARCHAR(20) DEFAULT 'UNPAID' COMMENT '支付状态（UNPAID-未支付 PAID-已支付 REFUNDING-退款中 REFUNDED-已退款）',
    payment_time DATETIME COMMENT '支付时间',
    refund_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '退款金额',
    refund_time DATETIME COMMENT '退款时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (appointment_id) REFERENCES tb_appointment(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    INDEX idx_appointment_id (appointment_id),
    INDEX idx_transaction_no (transaction_no),
    INDEX idx_payment_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- ============================================
-- 7. 评价表 (tb_review)
-- ============================================
CREATE TABLE tb_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
    appointment_id BIGINT NOT NULL COMMENT '预约ID（关联tb_appointment）',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    rating TINYINT NOT NULL COMMENT '评分（1-5分）',
    content TEXT COMMENT '评价内容',
    audit_status TINYINT DEFAULT 0 COMMENT '审核状态（0-待审核 1-已通过 2-已驳回）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (appointment_id) REFERENCES tb_appointment(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES tb_doctor(id) ON DELETE CASCADE,
    INDEX idx_appointment_id (appointment_id),
    INDEX idx_doctor_id (doctor_id),
    INDEX idx_audit_status (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

-- ============================================
-- 8. 系统日志表 (tb_operation_log)
-- ============================================
CREATE TABLE tb_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(50) COMMENT '操作用户名',
    operation_module VARCHAR(50) COMMENT '操作模块',
    operation_type VARCHAR(50) COMMENT '操作类型',
    request_method VARCHAR(10) COMMENT '请求方法',
    request_url VARCHAR(255) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_result TEXT COMMENT '响应结果',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    execution_time INT COMMENT '执行时长（毫秒）',
    status TINYINT COMMENT '状态（0-失败 1-成功）',
    error_msg TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_user_id (user_id),
    INDEX idx_operation_module (operation_module),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志表';

-- ============================================
-- 9. 系统配置表 (tb_system_config)
-- ============================================
CREATE TABLE tb_system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    config_type VARCHAR(50) COMMENT '配置类型',
    description VARCHAR(255) COMMENT '配置描述',
    status TINYINT DEFAULT 1 COMMENT '状态（0-禁用 1-启用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ============================================
-- 10. 数据字典表 (tb_dictionary)
-- ============================================
CREATE TABLE tb_dictionary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典ID',
    dict_type VARCHAR(50) NOT NULL COMMENT '字典类型',
    dict_code VARCHAR(50) NOT NULL COMMENT '字典编码',
    dict_label VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态（0-禁用 1-启用）',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_type_code (dict_type, dict_code),
    INDEX idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入管理员用户（密码: admin123 使用BCrypt加密）
INSERT INTO tb_user (username, password, real_name, phone, role_type) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', '13800138000', 'ADMIN');

-- 插入科室数据
INSERT INTO tb_department (dept_name, dept_code, dept_type, description, sort_order) VALUES
('内科', 'NK', '内科', '诊治各类内科疾病', 1),
('外科', 'WK', '外科', '诊治各类外科疾病', 2),
('儿科', 'EK', '儿科', '诊治儿童疾病', 3),
('妇产科', 'FCK', '妇产科', '诊治妇科和产科疾病', 4),
('骨科', 'GK', '外科', '诊治骨骼疾病', 5),
('眼科', 'YK', '五官科', '诊治眼部疾病', 6),
('耳鼻喉科', 'EBHK', '五官科', '诊治耳鼻喉疾病', 7),
('口腔科', 'KQK', '五官科', '诊治口腔疾病', 8),
('皮肤科', 'PFK', '皮肤科', '诊治皮肤疾病', 9),
('中医科', 'ZYK', '中医科', '中医诊疗', 10);

-- 插入数据字典
INSERT INTO tb_dictionary (dict_type, dict_code, dict_label, dict_value, sort_order) VALUES
('doctor_title', 'chief_physician', '主任医师', 'CHIEF_PHYSICIAN', 1),
('doctor_title', 'associate_chief', '副主任医师', 'ASSOCIATE_CHIEF', 2),
('doctor_title', 'attending_physician', '主治医师', 'ATTENDING_PHYSICIAN', 3),
('doctor_title', 'resident_physician', '住院医师', 'RESIDENT_PHYSICIAN', 4),
('time_slot', 'morning', '上午', 'MORNING', 1),
('time_slot', 'afternoon', '下午', 'AFTERNOON', 2),
('time_slot', 'evening', '晚间', 'EVENING', 3),
('gender', 'unknown', '未知', '0', 1),
('gender', 'male', '男', '1', 2),
('gender', 'female', '女', '2', 3);

-- 插入系统配置
INSERT INTO tb_system_config (config_key, config_value, config_type, description) VALUES
('appointment.advance.days', '7', 'INTEGER', '可提前预约天数'),
('appointment.cancel.hours', '2', 'INTEGER', '取消预约提前时间（小时）'),
('appointment.remind.hours', '2', 'INTEGER', '预约提醒提前时间（小时）'),
('payment.timeout.minutes', '30', 'INTEGER', '支付超时时间（分钟）'),
('system.name', '医院预约挂号系统', 'STRING', '系统名称');

-- ============================================
-- 创建视图（可选）
-- ============================================

-- 医生详情视图
CREATE OR REPLACE VIEW v_doctor_detail AS
SELECT 
    d.id AS doctor_id,
    d.user_id,
    u.real_name AS doctor_name,
    u.phone,
    u.gender,
    dept.id AS dept_id,
    dept.dept_name,
    dept.dept_type,
    d.title,
    d.specialty,
    d.introduction,
    d.consultation_fee,
    d.rating,
    d.consultation_count,
    d.status,
    d.create_time
FROM tb_doctor d
LEFT JOIN tb_user u ON d.user_id = u.id
LEFT JOIN tb_department dept ON d.dept_id = dept.id;

-- 预约详情视图
CREATE OR REPLACE VIEW v_appointment_detail AS
SELECT 
    a.id AS appointment_id,
    a.order_no,
    a.appointment_date,
    a.time_slot,
    a.queue_number,
    a.consultation_fee,
    a.appointment_status,
    a.payment_status,
    p.real_name AS patient_name,
    p.phone AS patient_phone,
    d.real_name AS doctor_name,
    doc.title AS doctor_title,
    dept.dept_name,
    a.create_time
FROM tb_appointment a
LEFT JOIN tb_user p ON a.patient_id = p.id
LEFT JOIN tb_doctor doc ON a.doctor_id = doc.id
LEFT JOIN tb_user d ON doc.user_id = d.id
LEFT JOIN tb_department dept ON a.dept_id = dept.id;

-- ============================================
-- 结束
-- ============================================

