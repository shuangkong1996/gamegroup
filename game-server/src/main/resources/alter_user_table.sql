-- 为已有 t_user 表添加下班时间字段
-- 执行此脚本前请确保已备份数据库

USE gamegroup;

-- 添加 off_work_hour 字段（在 phone 字段之后）
ALTER TABLE t_user 
ADD COLUMN off_work_hour INT DEFAULT 18 AFTER phone;

-- 添加 off_work_minute 字段（在 off_work_hour 字段之后）
ALTER TABLE t_user 
ADD COLUMN off_work_minute INT DEFAULT 0 AFTER off_work_hour;
