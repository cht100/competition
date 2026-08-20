package com.hrd.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 事件可视化视图对象
 * 用于前端展示事件及其包含的消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("事件可视化数据")
public class IncidentVisualVO implements Serializable {

    /**
     * 事件ID
     */
    @ApiModelProperty(value = "事件ID", example = "1")
    private Long id;

    /**
     * 事件摘要
     */
    @ApiModelProperty(value = "事件摘要", example = "某地区火灾事件")
    private String summary;

    /**
     * 灾害类型
     */
    @ApiModelProperty(value = "灾害类型", example = "火灾")
    private String disasterType;

    /**
     * 严重程度
     */
    @ApiModelProperty(value = "严重程度", example = "2")
    private Integer severity;

    /**
     * 事件状态
     */
    @ApiModelProperty(value = "事件状态", example = "1")
    private Integer status;

    /**
     * 状态文本
     */
    @ApiModelProperty(value = "状态文本", example = "已确认")
    private String statusText;

    /**
     * 位置文本
     */
    @ApiModelProperty(value = "位置文本", example = "某市某区某街道")
    private String locationText;

    /**
     * 事件中心点经度（用于可视化定位）
     */
    @ApiModelProperty(value = "经度", example = "116.3974")
    private Double lng;

    /**
     * 事件中心点纬度（用于可视化定位）
     */
    @ApiModelProperty(value = "纬度", example = "39.9093")
    private Double lat;

    /**
     * 事件创建时间
     */
    @ApiModelProperty(value = "创建时间", example = "2024-01-01T12:00:00")
    private String createTime;

    /**
     * 包含的消息列表
     */
    @ApiModelProperty(value = "消息列表")
    private List<MessageVisualVO> messages;

    /**
     * 消息数量
     */
    @ApiModelProperty(value = "消息数量", example = "5")
    private Integer messageCount;

    /**
     * 事件半径（用于可视化圆圈大小）
     */
    @ApiModelProperty(value = "事件半径", example = "50")
    private Integer radius;

    /**
     * 事件颜色（用于可视化）
     */
    @ApiModelProperty(value = "事件颜色", example = "#ff6b6b")
    private String color;
}