package com.muxin.scrm.module.ai.model.bo;

public record AiChatResult(
        String content,
        String provider,
        String model,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        int latencyMs
) {
}
