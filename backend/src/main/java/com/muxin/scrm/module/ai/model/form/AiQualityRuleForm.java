package com.muxin.scrm.module.ai.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "AI 质检规则表单")
public class AiQualityRuleForm {

    @NotBlank(message = "规则编码不能为空")
    private String code;

    @NotBlank(message = "规则名称不能为空")
    private String name;

    private String category;
    private String description;

    @NotBlank(message = "判定说明不能为空")
    private String instruction;

    private String severity;
    private Integer enabled;
    private Integer sort;
}
