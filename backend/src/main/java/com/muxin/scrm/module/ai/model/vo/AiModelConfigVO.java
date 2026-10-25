package com.muxin.scrm.module.ai.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "AI 模型配置")
public class AiModelConfigVO {

    private Long id;
    private String name;
    private String provider;
    private String baseUrl;
    private String model;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer enabled;
    private Integer isDefault;
    private Boolean hasApiKey;
    private String apiKeyMasked;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
