package com.muxin.scrm.module.ai.model.bo;

import java.math.BigDecimal;

public record AiRuntimeConfig(
        String provider,
        String baseUrl,
        String apiKey,
        String model,
        BigDecimal temperature,
        Integer maxTokens,
        boolean fallbackMock
) {
}
