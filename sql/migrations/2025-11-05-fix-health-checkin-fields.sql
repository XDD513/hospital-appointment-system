-- =============================================
-- 修复 health_checkin 表字段
-- 日期: 2025-11-05
-- 说明: 添加缺失的健康打卡字段
-- =============================================

USE tcm_health_system;

-- 检查并添加缺失的字段
SET @dbname = DATABASE();
SET @tablename = 'health_checkin';

-- 添加 plan_id 字段
SET @column_name = 'plan_id';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` BIGINT COMMENT ''计划ID（可选）'' AFTER `user_id`'),
    'SELECT ''Column plan_id already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 weight 字段
SET @column_name = 'weight';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` DECIMAL(5,2) COMMENT ''体重（kg）'' AFTER `checkin_type`'),
    'SELECT ''Column weight already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 blood_pressure 字段
SET @column_name = 'blood_pressure';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` VARCHAR(20) COMMENT ''血压（收缩压/舒张压）'' AFTER `weight`'),
    'SELECT ''Column blood_pressure already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 heart_rate 字段
SET @column_name = 'heart_rate';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` INT COMMENT ''心率（次/分）'' AFTER `blood_pressure`'),
    'SELECT ''Column heart_rate already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 sleep_duration 字段
SET @column_name = 'sleep_duration';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` DECIMAL(4,2) COMMENT ''睡眠时长（小时）'' AFTER `heart_rate`'),
    'SELECT ''Column sleep_duration already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 sleep_quality 字段
SET @column_name = 'sleep_quality';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` TINYINT COMMENT ''睡眠质量评分（1-5分）'' AFTER `sleep_duration`'),
    'SELECT ''Column sleep_quality already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 exercise_duration 字段
SET @column_name = 'exercise_duration';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` INT COMMENT ''运动时长（分钟）'' AFTER `sleep_quality`'),
    'SELECT ''Column exercise_duration already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 water_intake 字段
SET @column_name = 'water_intake';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` INT COMMENT ''饮水量（ml）'' AFTER `exercise_duration`'),
    'SELECT ''Column water_intake already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 mood_score 字段
SET @column_name = 'mood_score';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` TINYINT COMMENT ''心情评分（1-5分）'' AFTER `water_intake`'),
    'SELECT ''Column mood_score already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 health_score 字段
SET @column_name = 'health_score';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` TINYINT COMMENT ''身体状况评分（1-5分）'' AFTER `mood_score`'),
    'SELECT ''Column health_score already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 remark 字段
SET @column_name = 'remark';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` TEXT COMMENT ''备注'' AFTER `health_score`'),
    'SELECT ''Column remark already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 images 字段
SET @column_name = 'images';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` TEXT COMMENT ''图片（逗号分隔）'' AFTER `remark`'),
    'SELECT ''Column images already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 updated_at 字段
SET @column_name = 'updated_at';
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @column_name
);

SET @sql = IF(@column_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @column_name, '` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `created_at`'),
    'SELECT ''Column updated_at already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 重命名旧字段（如果存在）
-- checkin_content -> content
SET @old_column = 'checkin_content';
SET @new_column = 'content';
SET @old_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @old_column
);
SET @new_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
    AND TABLE_NAME = @tablename 
    AND COLUMN_NAME = @new_column
);

SET @sql = IF(@old_exists = 1 AND @new_exists = 0,
    CONCAT('ALTER TABLE `', @tablename, '` CHANGE COLUMN `', @old_column, '` `', @new_column, '` TEXT COMMENT ''打卡内容（JSON对象）'''),
    'SELECT ''Column rename not needed'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'health_checkin 表字段修复完成！' AS message;

