package com.muxin.scrm.module.ai.model.bo;

public record AiPromptPair(
        String code,
        String systemPrompt,
        String userTemplate,
        Integer version
) {
}
