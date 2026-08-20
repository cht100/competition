package com.hrd.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 创建执勤人员数据传输对象
 */
@Data
@ApiModel("创建执勤人员请求参数")
public class CreateResponderDTO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @ApiModelProperty(value = "用户名", example = "responder1", required = true)
    private String username;

    @NotBlank(message = "真实姓名不能为空")
    @ApiModelProperty(value = "真实姓名", example = "张三", required = true)
    private String name;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度必须在6-20位之间")
    @ApiModelProperty(value = "密码", example = "123456", required = true)
    private String password;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty(value = "手机号", example = "13800138000")
    private String phone;

    @ApiModelProperty(value = "岗位类型", example = "志愿者", allowableValues = "消防,医疗,警务,志愿者")
    private String position;
}