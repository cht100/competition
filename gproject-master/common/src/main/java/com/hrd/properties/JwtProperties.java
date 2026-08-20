package com.hrd.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hrd.jwt")
@Data
public class JwtProperties {

    /**
     * 管理端生成jwt令牌相关配置
     */
    private String adminSecretKey;
    private long adminTtl;
    private String adminTokenName;

    /**
     * 执勤人员端生成jwt令牌相关配置
     */
    private String responderSecretKey;
    private long responderTtl;
    private String responderTokenName;

}
