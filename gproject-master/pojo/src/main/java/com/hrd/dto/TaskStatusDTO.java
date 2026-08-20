package com.hrd.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 任务状态更新DTO
 * 用于接收任务状态更新的参数
 */
@Data
@ApiModel("任务状态更新参数")
public class TaskStatusDTO implements Serializable {

    /**
     * 任务状态：0-待出发,1-已出发,2-已到达,3-处理中,4-已完成,5-需增援
     */
    @ApiModelProperty(value = "任务状态", example = "0", allowableValues = "0,1,2,3,4,5")
    private Integer status;

    /**
     * 反馈备注
     */
    @ApiModelProperty(value = "反馈备注", example = "已到达现场，开始救援")
    private String feedback;
}
