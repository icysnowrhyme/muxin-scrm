package com.muxin.scrm.module.ai.service;

import cn.hutool.core.util.StrUtil;
import com.muxin.scrm.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiJsonService {

    private final ObjectMapper objectMapper;

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BusinessException("AI 结果序列化失败");
        }
    }

    /**
     * 模型经常返回 ```json 包裹或前后带说明文字，这里截取第一个对象。
     */
    public JsonNode readObject(String raw) {
        String json = extractJson(raw);
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new BusinessException("AI 返回内容不是合法 JSON，请检查模型或 Prompt 模板");
        }
    }

    public String extractJson(String raw) {
        if (StrUtil.isBlank(raw)) {
            throw new BusinessException("AI 返回内容为空");
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            if (firstLineEnd > 0) {
                text = text.substring(firstLineEnd + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException("AI 返回内容不是合法 JSON，请检查模型或 Prompt 模板");
        }
        return text.substring(start, end + 1);
    }

    public String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    public List<String> stringList(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return toStringList(value);
    }

    public List<String> toStringList(JsonNode value) {
        if (value == null || value.isNull()) {
            return List.of();
        }
        if (value.isArray()) {
            List<String> list = new ArrayList<>();
            value.forEach(item -> {
                if (item != null && !item.isNull() && StrUtil.isNotBlank(item.asText())) {
                    list.add(item.asText());
                }
            });
            return list;
        }
        if (StrUtil.isNotBlank(value.asText())) {
            return List.of(value.asText());
        }
        return List.of();
    }

    public List<String> parseStringList(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            return toStringList(objectMapper.readTree(json));
        } catch (Exception ex) {
            return List.of();
        }
    }
}
