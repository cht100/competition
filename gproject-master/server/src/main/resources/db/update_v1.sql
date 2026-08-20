-- ============================================
-- 应急指挥平台数据库更新脚本 v1
-- 更新日期：2026-03-04
-- 更新内容：
-- 1. 为incident表添加responder_id和feedback字段
-- 2. 为responder表添加current_incident_id字段
-- ============================================

-- --------------------------------------------
-- 1. 更新incident表，添加处理人员ID和反馈结果字段
-- --------------------------------------------
ALTER TABLE `incidents` 
ADD COLUMN `responder_id` BIGINT DEFAULT NULL COMMENT '处理人员ID' AFTER `status`,
ADD COLUMN `feedback` TEXT DEFAULT NULL COMMENT '处理反馈结果' AFTER `complete_time`;

-- 为responder_id字段添加索引
ALTER TABLE `incidents` 
ADD INDEX `idx_responder_id` (`responder_id`);

-- --------------------------------------------
-- 2. 更新responder表，添加当前处理事件ID字段
-- --------------------------------------------
ALTER TABLE `responders` 
ADD COLUMN `current_incident_id` BIGINT DEFAULT NULL COMMENT '当前处理的事件ID' AFTER `user_id`;

-- 为current_incident_id字段添加索引
ALTER TABLE `responders` 
ADD INDEX `idx_current_incident_id` (`current_incident_id`);

-- --------------------------------------------
-- 3. 添加外键约束（可选，根据实际需求决定）
-- --------------------------------------------
-- 注意：如果数据库已有数据，添加外键前需要确保数据一致性
-- 这里暂时不添加外键，避免影响现有数据

-- ALTER TABLE `incidents` 
-- ADD CONSTRAINT `fk_incident_responder` 
-- FOREIGN KEY (`responder_id`) REFERENCES `responders` (`id`) ON DELETE SET NULL;

-- ALTER TABLE `responders` 
-- ADD CONSTRAINT `fk_responder_current_incident` 
-- FOREIGN KEY (`current_incident_id`) REFERENCES `incidents` (`id`) ON DELETE SET NULL;

-- --------------------------------------------
-- 4. 更新注释说明
-- --------------------------------------------
-- 更新incident表的status字段注释，说明状态流转关系
ALTER TABLE `incidents` 
MODIFY COLUMN `status` TINYINT DEFAULT 0 COMMENT '事件状态：0-待审核,1-已确认,2-已派单,3-处理中,4-已完成,5-已驳回';

-- 更新responder表的status字段注释，说明状态流转关系
ALTER TABLE `responders` 
MODIFY COLUMN `status` TINYINT DEFAULT 0 COMMENT '状态：0-离线,1-空闲,2-已派单,3-执行中';

-- --------------------------------------------
-- 5. 验证更新结果
-- --------------------------------------------
-- 查看incident表结构
-- DESC `incidents`;

-- 查看responder表结构
-- DESC `responders`;

-- 查看新增的索引
-- SHOW INDEX FROM `incidents` WHERE Column_name = 'responder_id';
-- SHOW INDEX FROM `responders` WHERE Column_name = 'current_incident_id';

-- --------------------------------------------
-- 6. 回滚脚本（如果需要回滚）
-- --------------------------------------------
-- 如果需要回滚，执行以下SQL：
-- 
-- ALTER TABLE `incidents` 
-- DROP COLUMN `responder_id`,
-- DROP COLUMN `feedback`;
-- 
-- ALTER TABLE `responders` 
-- DROP COLUMN `current_incident_id`;