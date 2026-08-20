package com.hrd.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 智能派单请求数据传输对象
 */
@Data
@ApiModel("智能派单请求参数")
public class DispatchRequestDTO implements Serializable {

    @NotNull(message = "事件ID不能为空")
    @ApiModelProperty(value = "事件ID", example = "1", required = true)
    private Long incidentId;

    @ApiModelProperty(value = "是否强制派单", example = "false", notes = "true-强制派单，false-智能匹配")
    private Boolean forceDispatch = false;

    @ApiModelProperty(value = "指定执勤人员ID", example = "1", notes = "强制派单时指定执勤人员")
    private Long specifiedResponderId;
}