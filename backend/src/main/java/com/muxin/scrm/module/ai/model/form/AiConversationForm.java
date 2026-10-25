package com.muxin.scrm.module.ai.model.form;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "AI 会话导入表单")
public class AiConversationForm {

    private String title;

    private Long customerId;

    private String customerName;

    private String employeeName;

    @NotBlank(message = "会话内容不能为空")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    private String source;
}
