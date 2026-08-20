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
 * 事件证据链实体类
 * 关联事件和消息，形成证据链
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("incident_evidences") // 明确指定表名
public class IncidentEvidence implements Serializable {

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
     * 关联消息ID
     */
    private Long messageId;

    /**
     * 证据顺序
     */
    private Integer sortOrder;

    /**
     * 是否为主要证据：0-否,1-是
     */
    private Integer isPrimary;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}