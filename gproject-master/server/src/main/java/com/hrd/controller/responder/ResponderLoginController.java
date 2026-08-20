package com.hrd.controller.responder;

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
 * 执勤人员登录控制器
 */
@RestController
@RequestMapping("/responder/login")
@Slf4j
@Api(tags = "执勤人员登录")
public class ResponderLoginController {

    @Autowired
    private LoginService loginService;
    
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 执勤人员登录
     */
    @PostMapping
    @ApiOperation("执勤人员登录")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        log.info("执勤人员登录：{}", loginDTO);

        User user = loginService.login(loginDTO);

        // 检查用户角色是否为执勤人员
        if (user.getRole() != 2) {
            return Result.error("该账号不是执勤人员账号");
        }

        // 检查用户状态是否正常
        if (user.getStatus() != 1) {
            return Result.error("账号已被禁用，请联系管理员");
        }

        // 登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        String token = JwtUtil.createJWT(
                jwtProperties.getResponderSecretKey(),
                jwtProperties.getResponderTtl(),
                claims);

        LoginVO loginVO = LoginVO.builder()
                .id(user.getId())
                .userName(user.getUsername())
                .token(token)
                .build();

        return Result.success(loginVO);
    }
}