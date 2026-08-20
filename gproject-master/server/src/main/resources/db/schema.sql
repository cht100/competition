-- ============================================
-- 应急指挥平台数据库建表脚本
-- ============================================

-- --------------------------------------------
-- 1. 聚合事件表 (incidents)
-- 用于存储多条消息聚合后的事件卡片
-- --------------------------------------------
DROP TABLE IF EXISTS `incidents`;
CREATE TABLE `incidents` (
                             `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
                             `summary` VARCHAR(500) DEFAULT NULL COMMENT '事件摘要描述',
                             `disaster_type` VARCHAR(50) DEFAULT NULL COMMENT '灾害类型：洪涝/火灾/地震/滑坡/交通事故/燃气泄漏/电梯困人/道路塌陷',
                             `info_type` VARCHAR(50) DEFAULT NULL COMMENT '信息类型：求助/报警/现场目击/谣言/转载/无关',
                             `severity` TINYINT DEFAULT 0 COMMENT '严重程度：0-轻微,1-一般,2-严重,3-特别严重',
                             `confidence` INT DEFAULT 0 COMMENT '综合置信度（0-100）',
                             `location_text` VARCHAR(255) DEFAULT NULL COMMENT '位置描述',
                             `lat` DECIMAL(10, 7) DEFAULT NULL COMMENT '纬度',
                             `lng` DECIMAL(10, 7) DEFAULT NULL COMMENT '经度',
                             `status` TINYINT DEFAULT 0 COMMENT '事件状态：0-待审核,1-已确认,2-已派单,3-处理中,4-已完成,5-已驳回',
                             `ai_suggestion` TEXT DEFAULT NULL COMMENT 'AI研判结果JSON（包含建议处置、建议资源等）',
                             `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             `confirm_time` DATETIME DEFAULT NULL COMMENT '确认时间',
                             `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
                             PRIMARY KEY (`id`),
                             INDEX `idx_disaster_type` (`disaster_type`),
                             INDEX `idx_status` (`status`),
                             INDEX `idx_create_time` (`create_time`),
                             INDEX `idx_location` (`lat`, `lng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聚合事件表';

-- --------------------------------------------
-- 2. 执勤人员表 (responders)
-- --------------------------------------------
DROP TABLE IF EXISTS `responders`;
CREATE TABLE `responders` (
                              `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
                              `name` VARCHAR(50) NOT NULL COMMENT '人员姓名',
                              `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
                              `position` VARCHAR(50) DEFAULT NULL COMMENT '岗位类型：消防/医疗/警务/志愿者',
                              `status` TINYINT DEFAULT 0 COMMENT '状态：0-离线,1-空闲,2-已派单,3-执行中',
                              `lat` DECIMAL(10, 7) DEFAULT NULL COMMENT '当前纬度',
                              `lng` DECIMAL(10, 7) DEFAULT NULL COMMENT '当前经度',
                              `user_id` BIGINT DEFAULT NULL COMMENT '关联用户ID',
                              `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (`id`),
                              INDEX `idx_position` (`position`),
                              INDEX `idx_status` (`status`),
                              INDEX `idx_user_id` (`user_id`),
                              INDEX `idx_location` (`lat`, `lng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执勤人员表';

-- --------------------------------------------
-- 3. 派单任务表 (dispatch_tasks)
-- --------------------------------------------
DROP TABLE IF EXISTS `dispatch_tasks`;
CREATE TABLE `dispatch_tasks` (
                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
                                  `incident_id` BIGINT NOT NULL COMMENT '关联事件ID',
                                  `responder_id` BIGINT NOT NULL COMMENT '关联人员ID',
                                  `description` VARCHAR(500) DEFAULT NULL COMMENT '任务描述',
                                  `location` VARCHAR(255) DEFAULT NULL COMMENT '任务地点',
                                  `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
                                  `notes` VARCHAR(500) DEFAULT NULL COMMENT '注意事项',
                                  `status` TINYINT DEFAULT 0 COMMENT '任务状态：0-待出发,1-已出发,2-已到达,3-处理中,4-已完成,5-需增援',
                                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `depart_time` DATETIME DEFAULT NULL COMMENT '出发时间',
                                  `arrive_time` DATETIME DEFAULT NULL COMMENT '到达时间',
                                  `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
                                  `feedback` TEXT DEFAULT NULL COMMENT '反馈备注',
                                  PRIMARY KEY (`id`),
                                  INDEX `idx_incident_id` (`incident_id`),
                                  INDEX `idx_responder_id` (`responder_id`),
                                  INDEX `idx_status` (`status`),
                                  INDEX `idx_create_time` (`create_time`),
                                  CONSTRAINT `fk_dispatch_incident` FOREIGN KEY (`incident_id`) REFERENCES `incidents` (`id`) ON DELETE CASCADE,
                                  CONSTRAINT `fk_dispatch_responder` FOREIGN KEY (`responder_id`) REFERENCES `responders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='派单任务表';

-- --------------------------------------------
-- 4. 审核记录表 (review_logs)
-- --------------------------------------------
DROP TABLE IF EXISTS `review_logs`;
CREATE TABLE `review_logs` (
                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
                               `incident_id` BIGINT NOT NULL COMMENT '关联事件ID',
                               `operation` VARCHAR(50) NOT NULL COMMENT '操作类型：确认/驳回/修改/合并',
                               `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
                               `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
                               `remark` VARCHAR(500) DEFAULT NULL COMMENT '操作备注/原因',
                               `before_data` TEXT DEFAULT NULL COMMENT '操作前数据快照JSON',
                               `after_data` TEXT DEFAULT NULL COMMENT '操作后数据快照JSON',
                               `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                               PRIMARY KEY (`id`),
                               INDEX `idx_incident_id` (`incident_id`),
                               INDEX `idx_operator_id` (`operator_id`),
                               INDEX `idx_create_time` (`create_time`),
                               CONSTRAINT `fk_review_incident` FOREIGN KEY (`incident_id`) REFERENCES `incidents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核记录表';

-- --------------------------------------------
-- 5. 事件证据链表 (incident_evidences)
-- 关联事件和消息，形成证据链
-- --------------------------------------------
DROP TABLE IF EXISTS `incident_evidences`;
CREATE TABLE `incident_evidences` (
                                      `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
                                      `incident_id` BIGINT NOT NULL COMMENT '关联事件ID',
                                      `message_id` BIGINT NOT NULL COMMENT '关联消息ID',
                                      `sort_order` INT DEFAULT 0 COMMENT '证据顺序',
                                      `is_primary` TINYINT DEFAULT 0 COMMENT '是否为主要证据：0-否,1-是',
                                      `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      PRIMARY KEY (`id`),
                                      INDEX `idx_incident_id` (`incident_id`),
                                      INDEX `idx_message_id` (`message_id`),
                                      UNIQUE KEY `uk_incident_message` (`incident_id`, `message_id`),
                                      CONSTRAINT `fk_evidence_incident` FOREIGN KEY (`incident_id`) REFERENCES `incidents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件证据链表';

-- --------------------------------------------
-- 初始化测试数据（可选）
-- --------------------------------------------

-- 插入测试事件数据
-- INSERT INTO `incidents` (`summary`, `disaster_type`, `info_type`, `severity`, `confidence`, `location_text`, `lat`, `lng`, `status`)
-- VALUES ('某小区发生火灾', '火灾', '报警', 2, 85, '北京市朝阳区某某小区', 39.9042, 116.4074, 0);

-- 插入测试执勤人员数据
-- INSERT INTO `responders` (`name`, `phone`, `position`, `status`, `lat`, `lng`)
-- VALUES ('张三', '13800138001', '消防', 1, 39.9100, 116.4000);