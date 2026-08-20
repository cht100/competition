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

/**
 * 聚合事件实体类
 * 用于存储多条消息聚合后的事件卡片
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("incidents") // 明确指定表名
public class Incident implements Serializable {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 事件摘要描述
     */
    private String summary;

    /**
     * 灾害类型：洪涝/火灾/地震/滑坡/交通事故/燃气泄漏/电梯困人/道路塌陷
     */
    private String disasterType;

    /**
     * 信息类型：求助/报警/现场目击/转载/无关
     */
    private String infoType;

    /**
     * 严重程度：0-轻微,1-一般,2-严重,3-特别严重
     */
    private Integer severity;

    /**
     * 综合置信度（0-100）
     */
    private Integer confidence;

    /**
     * 位置描述
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
     * 事件状态：0-待审核,1-已确认,2-已派单,3-处理中,4-已完成,5-已驳回
     */
    private Integer status;

    /**
     * AI研判结果JSON（包含建议处置、建议资源等）
     */
    private String aiSuggestion;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 状态：待审核
     */
    public static final Integer STATUS_PENDING = 0;

    /**
     * 状态：已确认
     */
    public static final Integer STATUS_CONFIRMED = 1;

    /**
     * 状态：已派单
     */
    public static final Integer STATUS_DISPATCHED = 2;

    /**
     * 状态：处理中
     */
    public static final Integer STATUS_PROCESSING = 3;

    /**
     * 状态：已完成
     */
    public static final Integer STATUS_COMPLETED = 4;

    /**
     * 状态：已驳回
     */
    public static final Integer STATUS_REJECTED = 5;

    /**
     * 处理人员ID
     */
    private Long responderId;

    /**
     * 处理反馈结果
     */
    private String feedback;

}
