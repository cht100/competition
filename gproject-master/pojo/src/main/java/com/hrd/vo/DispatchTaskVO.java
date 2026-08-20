package com.hrd.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 派单任务VO类
 * 用于前端展示任务详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联事件ID
     */
    private Long incidentId;

    /**
     * 关联人员ID
     */
    private Long responderId;

    /**
     * 关联人员姓名
     */
    private String responderName;

    /**
     * 关联人员电话
     */
    private String responderPhone;

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
     * 任务状态：0-待出发,1-已出发,2-已到达,3-处理中,4-已完成,5-需增援
     */
    private Integer status;

    /**
     * 任务状态描述
     */
    private String statusDesc;

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
     * 事件摘要
     */
    private String incidentSummary;

    /**
     * 事件灾害类型
     */
    private String incidentDisasterType;

    /**
     * 获取状态描述
     */
    public String getStatusDesc() {
        if (this.status == null) {
            return "未知";
        }
        return switch (this.status) {
            case 0 -> "待出发";
            case 1 -> "已出发";
            case 2 -> "已到达";
            case 3 -> "处理中";
            case 4 -> "已完成";
            case 5 -> "需增援";
            default -> "未知";
        };
    }

    /**
     * 状态常量：待出发
     */
    public static final Integer STATUS_PENDING = 0;
    /**
     * 状态常量：已出发
     */
    public static final Integer STATUS_DEPARTED = 1;
    /**
     * 状态常量：已到达
     */
    public static final Integer STATUS_ARRIVED = 2;
    /**
     * 状态常量：处理中
     */
    public static final Integer STATUS_PROCESSING = 3;
    /**
     * 状态常量：已完成
     */
    public static final Integer STATUS_COMPLETED = 4;
    /**
     * 状态常量：需增援
     */
    public static final Integer STATUS_REINFORCE = 5;
}