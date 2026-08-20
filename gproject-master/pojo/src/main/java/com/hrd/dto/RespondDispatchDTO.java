package com.hrd.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 响应派单数据传输对象
 */
@Data
@ApiModel("响应派单请求参数")
public class RespondDispatchDTO implements Serializable {

    @NotNull(message = "派单任务ID不能为空")
    @ApiModelProperty(value = "派单任务ID", example = "1", required = true)
    private Long dispatchTaskId;

    @ApiModelProperty(value = "是否接受", example = "true", required = true)
    private Boolean accept;

    @ApiModelProperty(value = "拒绝原因", example = "距离太远，无法及时到达")
    private String rejectReason;
}