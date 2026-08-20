package com.hrd.controller.admin;

import com.hrd.dto.LoginDTO;
import com.hrd.entity.User;
import com.hrd.properties.JwtProperties;
import com.hrd.result.Result;
import com.hrd.service.LoginService;
import com.hrd.utils.JwtUtil;
import com.hrd.vo.LoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录控制器
 */
@RestController
@RequestMapping("/admin/login")
@Slf4j
@Api(tags = "登录管理")
public class LoginController {
    @Autowired
    private LoginService loginService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 管理员登录
     */
    @PostMapping
    @ApiOperation("管理员登录")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO){
        log.info("管理员登录：{}", loginDTO);

        User user = loginService.login(loginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        LoginVO loginVO = LoginVO.builder()
                .id(user.getId())
                .userName(user.getUsername())
                .token(token)
                .build();

        return Result.success(loginVO);
    }
}
