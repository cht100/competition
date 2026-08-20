package com.hrd.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO implements Serializable {
    /**
     * 主键ID，自增
     */
    private Long id;

    /**
     * 原始文本内容
     */
    private String originalText;

    /**
     * 清洗后的文本内容
     */
    private String cleanedText;

    /**
     * 发布人脱敏昵称
     */
    private String publisherName;

    /**
     * 发布人脱敏ID
     */
    private String publisherId;

    /**
     * 消息原始发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 系统接收时间
     */
    private LocalDateTime createTime;

    /**
     * 原始位置描述
     */
    private String locationText;

    /**
     * 纬度
     */
    private BigDecimal lat;

    /**
     * 经度
     */
    private BigDecimal lng;

    /**
     * 消息来源平台：微博/QQ/微信群/电话转写等
     */
    private String sourcePlatform;

    /**
     * 处理状态：0-待清洗,1-已清洗,2-已研判,3-已聚合,4-已忽略
     */
    private Integer status;

    /**
     * AI研判结果（JSON格式）
     */
    private String aiAnalysis;

    /**
     * 关联的聚合事件ID
     */
    private Long incidentId;

}
