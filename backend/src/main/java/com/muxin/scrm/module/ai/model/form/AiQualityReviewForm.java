package com.muxin.scrm.module.ai.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "AI 质检复核表单")
public class AiQualityReviewForm {

    @NotNull(message = "复核结果不能为空")
    @Schema(description = "1 确认违规，2 误报")
    private Integer reviewStatus;

    private String reviewNote;
}
