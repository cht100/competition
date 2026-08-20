package com.hrd.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 消息可视化视图对象
 * 用于前端展示消息在事件中的位置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("消息可视化数据")
public class MessageVisualVO implements Serializable {

    /**
     * 消息ID
     */
    @ApiModelProperty(value = "消息ID", example = "1")
    private Long id;

    /**
     * 消息内容
     */
    @ApiModelProperty(value = "消息内容", example = "某地发生火灾，需要救援")
    private String content;

    /**
     * 发布者名称
     */
    @ApiModelProperty(value = "发布者名称", example = "用户123")
    private String publisherName;

    /**
     * 消息类型
     */
    @ApiModelProperty(value = "消息类型", example = "求助")
    private String infoType;

    /**
     * 置信度
     */
    @ApiModelProperty(value = "置信度", example = "0.9")
    private Float confidence;

    /**
     * 消息时间
     */
    @ApiModelProperty(value = "消息时间", example = "2024-01-01T12:00:00")
    private String messageTime;

    /**
     * 在事件中的角度（用于可视化定位）
     */
    @ApiModelProperty(value = "角度", example = "45")
    private Double angle;

    /**
     * 在事件中的距离（用于可视化定位）
     */
    @ApiModelProperty(value = "距离", example = "30")
    private Double distance;

    /**
     * 消息颜色（用于可视化）
     */
    @ApiModelProperty(value = "消息颜色", example = "#4ecdc4")
    private String color;

    /**
     * 消息大小（用于可视化）
     */
    @ApiModelProperty(value = "消息大小", example = "10")
    private Integer size;

    /**
     * 是否是新消息（用于动画效果）
     */
    @ApiModelProperty(value = "是否是新消息", example = "true")
    private Boolean isNew;
}