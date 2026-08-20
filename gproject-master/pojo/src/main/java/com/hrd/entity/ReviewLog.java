package com.hrd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审核记录实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("review_logs") // 明确指定表名
public class ReviewLog implements Serializable {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联事件ID
     */
    private Long incidentId;

    /**
     * 操作类型：确认/驳回/修改/合并
     */
    private String operation;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作备注/原因
     */
    private String remark;

    /**
     * 操作前数据快照JSON
     */
    private String beforeData;

    /**
     * 操作后数据快照JSON
     */
    private String afterData;

    /**
     * 操作时间
     */
    private LocalDateTime createTime;

}
