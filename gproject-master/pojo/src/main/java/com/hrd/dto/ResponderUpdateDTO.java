package com.hrd.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 执勤人员更新信息数据传输对象
 */
@Data
@ApiModel("执勤人员更新信息请求参数")
public class ResponderUpdateDTO implements Serializable {

    @ApiModelProperty(value = "状态", example = "1", allowableValues = "0,1", notes = "0-离线,1-空闲")
    private Integer status;

    @ApiModelProperty(value = "岗位类型", example = "志愿者", allowableValues = "消防,医疗,警务,志愿者")
    private String position;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty(value = "手机号", example = "13800138000")
    private String phone;
}