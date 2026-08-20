-- 创建数据库
CREATE DATABASE IF NOT EXISTS gproject DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE gproject;

-- 消息表
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
    `original_text` TEXT DEFAULT NULL COMMENT '原始文本内容',
    `cleaned_text` TEXT DEFAULT NULL COMMENT '清洗后的文本内容',
    `images` VARCHAR(1000) DEFAULT NULL COMMENT '图片URL列表，JSON格式或逗号分隔',
    `publisher_name` VARCHAR(100) DEFAULT NULL COMMENT '发布人脱敏昵称',
    `publisher_id` VARCHAR(100) DEFAULT NULL COMMENT '发布人脱敏ID',
    `publish_time` DATETIME DEFAULT NULL COMMENT '消息原始发布时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '系统接收时间',
    `location_text` VARCHAR(255) DEFAULT NULL COMMENT '原始位置描述',
    `lat` DECIMAL(10, 7) DEFAULT NULL COMMENT '纬度',
    `lng` DECIMAL(10, 7) DEFAULT NULL COMMENT '经度',
    `source_platform` VARCHAR(50) DEFAULT NULL COMMENT '消息来源平台：微博/QQ/微信群/电话转写等',
    `url` VARCHAR(500) DEFAULT NULL COMMENT '原文链接（模拟可空）',
    `status` TINYINT DEFAULT 0 COMMENT '处理状态：0-待清洗,1-已清洗,2-已研判,3-已聚合,4-已忽略',
    `ai_analysis` TEXT DEFAULT NULL COMMENT 'AI研判结果（JSON格式）',
    `incident_id` BIGINT DEFAULT NULL COMMENT '关联的聚合事件ID',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_source_platform` (`source_platform`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_incident_id` (`incident_id`),
    INDEX `idx_location` (`lat`, `lng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `role` TINYINT DEFAULT 1 COMMENT '角色：1-管理员 2-执勤人员',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
    `duty_status` TINYINT DEFAULT 0 COMMENT '执勤状态：1-执勤中 0-休息中',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 聚合事件表
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
    `responder_id` BIGINT DEFAULT NULL COMMENT '负责人 ID',
    `feedback` TEXT DEFAULT NULL COMMENT '反馈备注',
    PRIMARY KEY (`id`),
    INDEX `idx_disaster_type` (`disaster_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_location` (`lat`, `lng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聚合事件表';

-- 执勤人员表
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
    `current_incident_id` BIGINT DEFAULT NULL COMMENT '当前处理的事件ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_position` (`position`),
    INDEX `idx_status` (`status`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_location` (`lat`, `lng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执勤人员表';

-- 派单任务表
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
    `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回原因',
    PRIMARY KEY (`id`),
    INDEX `idx_incident_id` (`incident_id`),
    INDEX `idx_responder_id` (`responder_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='派单任务表';

-- 审核记录表
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
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核记录表';

-- 事件证据链表
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
    UNIQUE KEY `uk_incident_message` (`incident_id`, `message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件证据链表';

-- 插入默认管理员用户（密码：123456）
INSERT INTO `users` (`id`, `username`, `password`, `phone`, `role`, `status`) VALUES 
(1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '13800000000', 1, 1),
(2, 'zhangwei', 'e10adc3949ba59abbe56e057f20f883e', '13800138001', 2, 1),
(3, 'liming', 'e10adc3949ba59abbe56e057f20f883e', '13800138002', 2, 1),
(4, 'wangqiang', 'e10adc3949ba59abbe56e057f20f883e', '13800138003', 2, 1),
(5, 'liuyang', 'e10adc3949ba59abbe56e057f20f883e', '13800138004', 2, 1),
(6, 'chengang', 'e10adc3949ba59abbe56e057f20f883e', '13800138005', 2, 1);

-- 插入测试执勤人员数据（关联用户账号）
INSERT INTO `responders` (`name`, `phone`, `position`, `status`, `lat`, `lng`, `user_id`) VALUES 
('张伟', '13800138001', '消防', 1, 39.908823, 116.397470, 2),
('李明', '13800138002', '医疗', 1, 39.912345, 116.401234, 3),
('王强', '13800138003', '警务', 1, 39.915678, 116.405678, 4),
('刘洋', '13800138004', '志愿者', 1, 39.920123, 116.410123, 5),
('陈刚', '13800138005', '消防', 1, 39.923456, 116.413456, 6);

-- 插入测试事件数据
INSERT INTO `incidents` (`summary`, `disaster_type`, `info_type`, `severity`, `confidence`, `location_text`, `lat`, `lng`, `status`) VALUES 
('滨河小区3号楼2单元发生洪涝，一楼进水，老人小孩被困楼道', '洪涝', '求助', 3, 92, '滨河小区3号楼2单元', 39.908823, 116.397470, 0),
('城南工业园区A区3号厂房发生火灾，明火直窜二楼', '火灾', '报警', 3, 95, '城南工业园区A区3号厂房', 39.923456, 116.413456, 0),
('东风路与人民路交叉口发生交通事故，货车翻车', '交通事故', '求助', 2, 88, '东风路与人民路交叉口', 39.950123, 116.440123, 1);
