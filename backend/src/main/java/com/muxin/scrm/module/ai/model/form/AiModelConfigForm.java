package com.muxin.scrm.module.ai.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "AI 模型配置表单")
public class AiModelConfigForm {

    @NotBlank(message = "配置名称不能为空")
    private String name;

    @NotBlank(message = "模型提供方不能为空")
    private String provider;

    private String baseUrl;

    @Schema(description = "新的 API Key；留空表示保持不变，__CLEAR__ 表示清空")
    private String apiKey;

    @NotBlank(message = "模型名称不能为空")
    private String model;

    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer enabled;
    private Integer isDefault;
}
