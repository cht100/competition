package com.hrd.dto;

import com.hrd.entity.DispatchTask;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 派单结果数据传输对象
 */
@Data
@Builder
@ApiModel("派单结果")
public class DispatchResultDTO implements Serializable {

    @ApiModelProperty(value = "是否派单成功", example = "true")
    private Boolean success;

    @ApiModelProperty(value = "派单结果消息", example = "派单成功，已通知执勤人员")
    private String message;

    @ApiModelProperty(value = "派单任务信息")
    private DispatchTask dispatchTask;

    @ApiModelProperty(value = "匹配的执勤人员数量", example = "3")
    private Integer matchedResponderCount;

    @ApiModelProperty(value = "匹配的执勤人员名称")
    private String responderName;

    @ApiModelProperty(value = "匹配理由")
    private String reason;
}