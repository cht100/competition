package com.hrd.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("users") // 明确指定表名
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String password;

    private String phone;

    private Integer role;//1-管理员 2-执勤人员

    private Integer status;//1-正常 0-禁用

    private Integer dutyStatus;//1-执勤中 0-休息中

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
