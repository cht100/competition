package com.hrd.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 接受任务数据传输对象
 */
@Data
@ApiModel("接受任务请求参数")
public class AcceptTaskDTO implements Serializable {

    @NotNull(message = "事件ID不能为空")
    @ApiModelProperty(value = "事件ID", example = "1", required = true)
    private Long incidentId;
}