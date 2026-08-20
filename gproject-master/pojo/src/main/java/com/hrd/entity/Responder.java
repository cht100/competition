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
 * 执勤人员实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("responders") // 明确指定表名
public class Responder implements Serializable {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 人员姓名
     */
    private String name;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 岗位类型：消防/医疗/警务/志愿者
     */
    private String position;

    /**
     * 状态：0-离线,1-空闲,2-已派单,3-执行中
     */
    private Integer status;

    /**
     * 当前纬度
     */
    private BigDecimal lat;

    /**
     * 当前经度
     */
    private BigDecimal lng;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 状态：离线
     */
    public static final Integer STATUS_OFFLINE = 0;

    /**
     * 状态：空闲
     */
    public static final Integer STATUS_IDLE = 1;

    /**
     * 状态：已派单
     */
    public static final Integer STATUS_DISPATCHED = 2;

    /**
     * 状态：执行中
     */
    public static final Integer STATUS_EXECUTING = 3;

    /**
     * 当前处理的事件ID
     */
    private Long currentIncidentId;

}
