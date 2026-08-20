package com.hrd.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 提交反馈数据传输对象
 */
@Data
@ApiModel("提交反馈请求参数")
public class SubmitFeedbackDTO implements Serializable {

    @NotNull(message = "事件ID不能为空")
    @ApiModelProperty(value = "事件ID", example = "1", required = true)
    private Long incidentId;

    @NotBlank(message = "反馈内容不能为空")
    @ApiModelProperty(value = "处理反馈结果", example = "已到达现场，火势已控制，无人员伤亡", required = true)
    private String feedback;
}