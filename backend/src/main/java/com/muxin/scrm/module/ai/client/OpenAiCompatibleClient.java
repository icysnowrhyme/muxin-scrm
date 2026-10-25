package com.muxin.scrm.module.ai.client;

import cn.hutool.core.util.StrUtil;
import com.muxin.scrm.core.exception.BusinessException;
import com.muxin.scrm.module.ai.config.AiProperties;
import com.muxin.scrm.module.ai.model.bo.AiChatRequest;
import com.muxin.scrm.module.ai.model.bo.AiChatResult;
import com.muxin.scrm.module.ai.model.bo.AiRuntimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 兼容 OpenAI Chat Completions 协议的客户端，可接 DeepSeek、通义千问、豆包等兼容网关。
 */
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleClient implements AiClient {

    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    @Override
    public AiChatResult chat(AiChatRequest request, AiRuntimeConfig config) {
        long start = System.currentTimeMillis();
        String endpoint = endpoint(config.baseUrl());
        Duration timeout = Duration.ofSeconds(Math.max(properties.getTimeoutSeconds(), 5));

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.model());
        body.put("temperature", request.temperature());
        body.put("max_tokens", request.maxTokens());
        body.put("stream", false);
        ArrayNode messages = body.putArray("messages");
        if (StrUtil.isNotBlank(request.systemPrompt())) {
            messages.addObject().put("role", "system").put("content", request.systemPrompt());
        }
        messages.addObject().put("role", "user").put("content", request.userPrompt());

        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("模型接口返回 " + response.statusCode() + "："
                        + StrUtil.maxLength(StrUtil.blankToDefault(response.body(), "无响应内容"), 300));
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choice = root.path("choices").path(0);
            String content = choice.path("message").path("content").asText(null);
            if (StrUtil.isBlank(content)) {
                content = choice.path("text").asText(null);
            }
            if (StrUtil.isBlank(content)) {
                throw new BusinessException("模型接口未返回内容");
            }
            JsonNode usage = root.path("usage");
            int promptTokens = usage.path("prompt_tokens").asInt(0);
            int completionTokens = usage.path("completion_tokens").asInt(0);
            int totalTokens = usage.path("total_tokens").asInt(promptTokens + completionTokens);
            int latency = (int) (System.currentTimeMillis() - start);
            return new AiChatResult(content, config.provider(), config.model(),
                    promptTokens, completionTokens, totalTokens, latency);
        } catch (BusinessException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("模型调用被中断");
        } catch (Exception ex) {
            throw new BusinessException("调用模型失败：" + ex.getMessage());
        }
    }

    private String endpoint(String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            throw new BusinessException("请先配置模型接口地址");
        }
        String url = baseUrl.trim().replaceAll("/+$", "");
        return url.endsWith("/chat/completions") ? url : url + "/chat/completions";
    }
}
