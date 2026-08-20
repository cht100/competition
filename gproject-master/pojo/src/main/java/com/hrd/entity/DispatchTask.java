package com.hrd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 派单任务实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dispatch_tasks")
public class DispatchTask {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联事件ID
     */
    private Long incidentId;

    /**
     * 关联执勤人员ID
     */
    private Long responderId;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务地点
     */
    private String location;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 注意事项
     */
    private String notes;

    /**
     * 任务状态
     */
    private Integer status;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 出发时间
     */
    private LocalDateTime departTime;

    /**
     * 到达时间
     */
    private LocalDateTime arriveTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 反馈备注
     */
    private String feedback;

    /**
     * 状态：待接受
     */
    public static final Integer STATUS_PENDING = 0;

    /**
     * 状态：已接受
     */
    public static final Integer STATUS_ACCEPTED = 1;

    /**
     * 状态：已拒绝
     */
    public static final Integer STATUS_REJECTED = 2;

    /**
     * 状态：已出发
     */
    public static final Integer STATUS_DEPARTED = 3;

    /**
     * 状态：已到达
     */
    public static final Integer STATUS_ARRIVED = 4;

    /**
     * 状态：处理中
     */
    public static final Integer STATUS_PROCESSING = 5;

    /**
     * 状态：已完成
     */
    public static final Integer STATUS_COMPLETED = 6;

    /**
     * 状态：需增援
     */
    public static final Integer STATUS_NEED_REINFORCEMENT = 7;

    /**
     * 状态：志愿者申请待审批
     */
    public static final Integer STATUS_VOLUNTEER_PENDING = 8;
}