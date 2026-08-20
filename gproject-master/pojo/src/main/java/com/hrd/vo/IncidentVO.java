package com.hrd.vo;

import com.hrd.entity.Incident;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 事件VO类
 * 用于前端展示事件详情，包含证据链和关联任务
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IncidentVO extends Incident implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 证据消息列表
     * 包含该事件关联的所有消息证据
     */
    private List<MessageVO> evidences;

    /**
     * 关联的任务列表
     * 包含该事件派发的所有处置任务
     */
    private List<DispatchTaskVO> tasks;

    /**
     * 证据消息数量
     */
    private Integer evidenceCount;

    /**
     * 关联任务数量
     */
    private Integer taskCount;

    /**
     * 事件严重程度描述
     */
    private String severityDesc;

    /**
     * 事件状态描述
     */
    private String statusDesc;

    /**
     * 获取严重程度描述
     */
    public String getSeverityDesc() {
        if (this.getSeverity() == null) {
            return "未知";
        }
        return switch (this.getSeverity()) {
            case 0 -> "轻微";
            case 1 -> "一般";
            case 2 -> "严重";
            case 3 -> "特别严重";
            default -> "未知";
        };
    }

    /**
     * 获取状态描述
     */
    public String getStatusDesc() {
        if (this.getStatus() == null) {
            return "未知";
        }
        return switch (this.getStatus()) {
            case 0 -> "待审核";
            case 1 -> "已确认";
            case 2 -> "已派单";
            case 3 -> "处理中";
            case 4 -> "已完成";
            case 5 -> "已驳回";
            default -> "未知";
        };
    }

    /**
     * 获取证据数量
     */
    public Integer getEvidenceCount() {
        return evidences != null ? evidences.size() : 0;
    }

    /**
     * 获取任务数量
     */
    public Integer getTaskCount() {
        return tasks != null ? tasks.size() : 0;
    }
}
