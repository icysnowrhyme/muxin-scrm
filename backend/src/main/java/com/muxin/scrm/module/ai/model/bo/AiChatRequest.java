package com.muxin.scrm.module.ai.model.bo;

import java.util.Map;

public record AiChatRequest(
        String feature,
        String systemPrompt,
        String userPrompt,
        double temperature,
        int maxTokens,
        Map<String, Object> context
) {
}
