-- =============================================
-- 修复 schedule 表的 booked_quota 字段
-- 日期: 2025-11-09
-- 说明: 根据 total_quota 和 remaining_quota 计算并更新 booked_quota
-- =============================================

USE tcm_health_system;

-- 更新所有排班记录的 booked_quota
-- booked_quota = total_quota - remaining_quota
UPDATE `schedule`
SET `booked_quota` = `total_quota` - `remaining_quota`
WHERE `booked_quota` != (`total_quota` - `remaining_quota`);

-- 验证更新结果
SELECT 
    COUNT(*) as total_records,
    SUM(CASE WHEN booked_quota = (total_quota - remaining_quota) THEN 1 ELSE 0 END) as correct_records,
    SUM(CASE WHEN booked_quota != (total_quota - remaining_quota) THEN 1 ELSE 0 END) as incorrect_records
FROM `schedule`;

-- 显示修复后的示例数据
SELECT 
    id,
    doctor_id,
    schedule_date,
    time_slot,
    total_quota,
    booked_quota,
    remaining_quota,
    (total_quota - remaining_quota) as calculated_booked
FROM `schedule`
WHERE schedule_date >= CURDATE()
ORDER BY schedule_date ASC, time_slot ASC
LIMIT 10;

SELECT '✅ schedule 表的 booked_quota 字段修复完成！' AS message;

