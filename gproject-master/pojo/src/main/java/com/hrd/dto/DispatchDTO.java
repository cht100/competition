package com.hrd.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 派单请求DTO
 * 用于接收派单请求的参数
 */
@Data
@ApiModel("派单请求参数")
public class DispatchDTO implements Serializable {

    /**
     * 事件ID
     */
    @ApiModelProperty(value = "事件ID", example = "1")
    private Long incidentId;

    /**
     * 执勤人员ID
     */
    @ApiModelProperty(value = "执勤人员ID", example = "1")
    private Long responderId;

    /**
     * 任务描述
     */
    @ApiModelProperty(value = "任务描述", example = "前往现场进行救援")
    private String description;

    /**
     * 截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime deadline;

    /**
     * 注意事项
     */
    private String notes;
}
