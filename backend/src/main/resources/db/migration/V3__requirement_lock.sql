-- 并发控制：需求表增加乐观锁版本列
ALTER TABLE requirement ADD COLUMN lock_version INT DEFAULT 0;
