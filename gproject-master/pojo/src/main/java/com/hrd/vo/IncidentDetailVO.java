package com.hrd.vo;

import com.hrd.entity.Incident;
import com.hrd.entity.Message;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件详情VO
 * 包含事件基本信息和证据链（关联的消息列表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("事件详情")
public class IncidentDetailVO {

    @ApiModelProperty(value = "事件ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "事件摘要", example = "某地区火灾事件")
    private String summary;

    @ApiModelProperty(value = "灾害类型", example = "火灾")
    private String disasterType;

    @ApiModelProperty(value = "信息类型", example = "求助")
    private String infoType;

    @ApiModelProperty(value = "严重程度", example = "2")
    private Integer severity;

    @ApiModelProperty(value = "综合置信度", example = "85")
    private Integer confidence;

    @ApiModelProperty(value = "位置描述", example = "某市某区某街道")
    private String locationText;

    @ApiModelProperty(value = "纬度", example = "39.9093")
    private BigDecimal lat;

    @ApiModelProperty(value = "经度", example = "116.3974")
    private BigDecimal lng;

    @ApiModelProperty(value = "事件状态", example = "1")
    private Integer status;

    @ApiModelProperty(value = "状态文本", example = "已确认")
    private String statusText;

    @ApiModelProperty(value = "AI研判建议")
    private String aiSuggestion;

    @ApiModelProperty(value = "创建时间", example = "2024-01-01T10:00:00")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间", example = "2024-01-01T10:30:00")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "确认时间", example = "2024-01-01T10:15:00")
    private LocalDateTime confirmTime;

    @ApiModelProperty(value = "完成时间")
    private LocalDateTime completeTime;

    @ApiModelProperty(value = "证据链（关联的消息列表）")
    private List<Message> evidenceChain;

    @ApiModelProperty(value = "消息数量", example = "5")
    private Integer messageCount;

    /**
     * 从Incident实体和消息列表构建IncidentDetailVO
     *
     * @param incident 事件实体
     * @param messages 关联的消息列表
     * @return IncidentDetailVO
     */
    public static IncidentDetailVO fromIncidentAndMessages(Incident incident, List<Message> messages) {
        return IncidentDetailVO.builder()
                .id(incident.getId())
                .summary(incident.getSummary())
                .disasterType(incident.getDisasterType())
                .infoType(incident.getInfoType())
                .severity(incident.getSeverity())
                .confidence(incident.getConfidence())
                .locationText(incident.getLocationText())
                .lat(incident.getLat())
                .lng(incident.getLng())
                .status(incident.getStatus())
                .statusText(getStatusText(incident.getStatus()))
                .aiSuggestion(incident.getAiSuggestion())
                .createTime(incident.getCreateTime())
                .updateTime(incident.getUpdateTime())
                .confirmTime(incident.getConfirmTime())
                .completeTime(incident.getCompleteTime())
                .evidenceChain(messages)
                .messageCount(messages != null ? messages.size() : 0)
                .build();
    }

    /**
     * 获取状态文本
     *
     * @param status 状态码
     * @return 状态文本
     */
    private static String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已确认";
            case 2 -> "已派单";
            case 3 -> "处理中";
            case 4 -> "已完成";
            case 5 -> "已驳回";
            default -> "未知";
        };
    }
}