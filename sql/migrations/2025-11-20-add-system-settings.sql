-- 补齐 system_config 表缺失的系统设置项
INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'notification.appointment_reminder', 'true', '是否开启预约提醒通知', 'notification', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'notification.appointment_reminder');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'notification.reminder_hours', '2', '提醒提前小时数', 'notification', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'notification.reminder_hours');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'notification.sms_enabled', 'false', '短信通知开关', 'notification', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'notification.sms_enabled');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'notification.email_enabled', 'false', '邮件通知开关', 'notification', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'notification.email_enabled');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'notification.system_enabled', 'true', '系统内通知开关', 'notification', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'notification.system_enabled');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'security.min_password_length', '8', '密码最小长度', 'security', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'security.min_password_length');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'security.login_lock_enabled', 'true', '登录失败锁定开关', 'security', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'security.login_lock_enabled');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'security.max_login_attempts', '5', '最大登录失败次数', 'security', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'security.max_login_attempts');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'security.lock_duration', '15', '登录锁定时间(分钟)', 'security', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'security.lock_duration');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'security.session_timeout', '120', '会话超时时间(分钟)', 'security', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'security.session_timeout');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'email.smtp_host', '', 'SMTP服务器地址', 'email', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'email.smtp_host');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'email.smtp_port', '587', 'SMTP端口', 'email', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'email.smtp_port');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'email.from_email', '', '发件邮箱', 'email', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'email.from_email');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'email.password', '', '发件邮箱密码', 'email', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'email.password');

INSERT INTO system_config (config_key, config_value, config_desc, config_type, created_at, updated_at)
SELECT 'email.ssl_enabled', 'true', 'SMTP SSL 开关', 'email', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'email.ssl_enabled');

