package com.muxin.scrm.module.ai.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI 质检任务表单")
public class AiQualityTaskForm {

    private String name;

    @NotEmpty(message = "请选择要质检的会话")
    private List<Long> conversationIds;

    @Schema(description = "规则 ID；为空表示使用全部启用规则")
    private List<Long> ruleIds;
}
