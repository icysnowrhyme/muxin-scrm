package com.muxin.scrm.module.ai.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "AI Prompt 模板表单")
public class AiPromptTemplateForm {

    @NotBlank(message = "模板名称不能为空")
    private String name;

    @NotBlank(message = "System Prompt 不能为空")
    private String systemPrompt;

    @NotBlank(message = "User Prompt 不能为空")
    private String userTemplate;

    private Integer enabled;
}
