package com.hrd.interceptor;

import com.hrd.context.BaseContext;
import com.hrd.properties.JwtProperties;
import com.hrd.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class JwtTokenResponderInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 校验执勤人员端jwt令牌
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取执勤人员令牌
        String token = request.getHeader(jwtProperties.getResponderTokenName());

        //2、校验令牌
        try {
            log.info("执勤人员端jwt校验: {}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getResponderSecretKey(), token);
            Long userId = Long.valueOf(claims.get("userId").toString());
            Integer role = Integer.valueOf(claims.get("role").toString());
            
            // 检查用户角色是否为执勤人员
            if (role != 2) {
                log.warn("非执勤人员角色访问执勤人员端接口: userId={}, role={}", userId, role);
                response.setStatus(403);
                return false;
            }
            
            BaseContext.setCurrentId(userId);
            log.info("当前执勤人员userId：{}", userId);
            
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            //4、不通过，响应401状态码
            response.setStatus(401);
            return false;
        }
    }
}