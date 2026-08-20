package com.hrd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("messages") // 明确指定表名
public class Message implements Serializable {


    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 原始文本内容
     */
    private String originalText;

    /**
     * 翻译后的文本内容
     */
    private String cleanedText;

    /**
     * 图片URL列表，JSON格式或逗号分隔
     */
    private String images;

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
     * 原文链接（模拟可空）
     */
    private String url;

    /**
     * 处理状态：0-待清洗,1-已清洗,2-已研判,3-已聚合,4-已忽略,5-谣言
     */
    private Integer status;

    /**
     * AI研判结果（JSON格式）：存储大模型返回的事件类型、置信度、建议资源等
     */
    private String aiAnalysis;

    /**
     * 关联的聚合事件ID（聚类后填入）
     */
    private Long incidentId;

    /**
     * 状态：待清洗
     */
    public static final Integer STATUS_TO_CLEAN = 0;
    /**
     * 状态：已清洗
     */
    public static final Integer STATUS_CLEANED = 1;
    /**
     * 状态：已研判
     */
    public static final Integer STATUS_JUDGED = 2;
    /**
     * 状态：已聚合
     */
    public static final Integer STATUS_AGGREGATED = 3;
    /**
     * 状态：已忽略
     */
    public static final Integer STATUS_IGNORED = 4;
    /**
     * 状态：谣言
     */
    public static final Integer STATUS_RUMOR = 5;

}
