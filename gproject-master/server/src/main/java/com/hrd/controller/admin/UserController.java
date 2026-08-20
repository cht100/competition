package com.hrd.controller.admin;

import com.hrd.annotation.AutoFill;
import com.hrd.dto.CreateResponderDTO;
import com.hrd.entity.User;
import com.hrd.result.Result;
import com.hrd.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/admin/user")
@Slf4j
@Api(tags = "用户管理")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @ApiOperation("获取用户信息")
    public Result<User> get() {
        return Result.success(User.builder()
                .id(1L)
                .username("test")
                .password("123456")
                .build());
    }

    /**
     * 创建执勤人员
     * 同时创建User和对应的Responder信息
     * @param createResponderDTO 创建执勤人员参数
     * @return 操作结果
     */
    @PostMapping("/responder")
    @ApiOperation("创建执勤人员")
    public Result<User> createResponder(@RequestBody CreateResponderDTO createResponderDTO) {
        log.info("创建执勤人员：{}", createResponderDTO);
        User user = userService.createResponder(createResponderDTO);
        return Result.success(user);
    }
}
