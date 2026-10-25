package com.muxin.scrm.module.ai.service;

import com.muxin.scrm.core.exception.BusinessException;
import com.muxin.scrm.module.ai.client.AiClient;
import com.muxin.scrm.module.ai.client.AiClientFactory;
import com.muxin.scrm.module.ai.model.bo.AiChatRequest;
import com.muxin.scrm.module.ai.model.bo.AiChatResult;
import com.muxin.scrm.module.ai.model.bo.AiRuntimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiGatewayService {

    private final AiModelConfigService modelConfigService;
    private final AiClientFactory clientFactory;
    private final AiUsageService usageService;

    public AiChatResult chat(AiChatRequest request) {
        AiRuntimeConfig runtime = modelConfigService.getRuntimeConfig();
        AiClient client = clientFactory.get(runtime);
        long start = System.currentTimeMillis();
        try {
            AiChatResult result = client.chat(request, runtime);
            usageService.recordSuccess(request.feature(), result);
            return result;
        } catch (RuntimeException ex) {
            usageService.recordFailure(request.feature(), runtime,
                    (int) (System.currentTimeMillis() - start), ex.getMessage());
            if (Boolean.TRUE.equals(runtime.fallbackMock()) && clientFactory.isReal(runtime)) {
                AiRuntimeConfig fallback = new AiRuntimeConfig("mock", null, null,
                        runtime.model(), runtime.temperature(), runtime.maxTokens(), true);
                AiChatResult result = clientFactory.mock().chat(request, fallback);
                usageService.recordSuccess(request.feature() + "-fallback", result);
                return result;
            }
            throw ex instanceof BusinessException be ? be : new BusinessException("AI 调用失败：" + ex.getMessage());
        }
    }
}
