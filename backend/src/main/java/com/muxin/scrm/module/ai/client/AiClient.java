package com.muxin.scrm.module.ai.client;

import com.muxin.scrm.module.ai.model.bo.AiChatRequest;
import com.muxin.scrm.module.ai.model.bo.AiChatResult;
import com.muxin.scrm.module.ai.model.bo.AiRuntimeConfig;

public interface AiClient {

    AiChatResult chat(AiChatRequest request, AiRuntimeConfig config);
}
