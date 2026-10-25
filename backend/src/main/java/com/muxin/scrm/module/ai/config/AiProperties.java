package com.muxin.scrm.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** API Key 加密口令，生产环境务必通过 AI_ENCRYPT_KEY 注入 */
    private String encryptKey = "muxin-local-ai-key";

    private Integer timeoutSeconds = 60;

    /** 真实模型调用失败时是否回落到 Mock，保证演示与页面可用 */
    private Boolean fallbackMock = true;
}
