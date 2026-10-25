package com.muxin.scrm.module.ai.client;

import cn.hutool.core.util.StrUtil;
import com.muxin.scrm.module.ai.model.bo.AiRuntimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiClientFactory {

    private final MockAiClient mockAiClient;
    private final OpenAiCompatibleClient openAiCompatibleClient;

    public AiClient get(AiRuntimeConfig config) {
        return isReal(config) ? openAiCompatibleClient : mockAiClient;
    }

    public boolean isReal(AiRuntimeConfig config) {
        return "openai".equalsIgnoreCase(config.provider())
                && StrUtil.isNotBlank(config.baseUrl())
                && StrUtil.isNotBlank(config.apiKey());
    }

    public AiClient mock() {
        return mockAiClient;
    }
}
