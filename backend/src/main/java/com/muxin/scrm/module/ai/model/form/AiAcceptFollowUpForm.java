package com.muxin.scrm.module.ai.model.form;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "采纳为跟进记录表单")
public class AiAcceptFollowUpForm {

    @Schema(description = "跟进内容；留空则使用 AI 生成的摘要")
    private String content;

    private String result;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextFollowTime;
}
