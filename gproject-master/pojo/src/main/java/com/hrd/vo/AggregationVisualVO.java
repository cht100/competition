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
 * 聚合可视化视图对象
 * 用于前端展示聚合过程的实时数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("聚合可视化数据")
public class AggregationVisualVO implements Serializable {

    /**
     * 聚合操作类型
     * NEW_EVENT - 创建新事件
     * MERGE_TO_EVENT - 合并到现有事件
     * SIMILARITY_CALCULATION - 相似度计算
     */
    @ApiModelProperty(value = "聚合操作类型", example = "NEW_EVENT")
    private String operationType;

    /**
     * 新消息ID
     */
    @ApiModelProperty(value = "新消息ID", example = "1")
    private Long newMessageId;

    /**
     * 新消息内容
     */
    @ApiModelProperty(value = "新消息内容", example = "某地发生火灾，需要救援")
    private String newMessageContent;

    /**
     * 目标事件ID（如果合并到现有事件）
     */
    @ApiModelProperty(value = "目标事件ID", example = "1")
    private Long targetIncidentId;

    /**
     * 目标事件摘要
     */
    @ApiModelProperty(value = "目标事件摘要", example = "某地区火灾事件")
    private String targetIncidentSummary;

    /**
     * 相似度分数
     */
    @ApiModelProperty(value = "相似度分数", example = "0.85")
    private Float similarityScore;

    /**
     * 是否超过阈值
     */
    @ApiModelProperty(value = "是否超过阈值", example = "true")
    private Boolean exceedThreshold;

    /**
     * 相似度阈值
     */
    @ApiModelProperty(value = "相似度阈值", example = "0.75")
    private Float similarityThreshold;

    /**
     * 事件列表（包含所有事件及其消息）
     */
    @ApiModelProperty(value = "事件列表")
    private List<IncidentVisualVO> incidents;

    /**
     * 聚合时间戳
     */
    @ApiModelProperty(value = "聚合时间戳", example = "2024-01-01T12:00:00")
    private String timestamp;
}