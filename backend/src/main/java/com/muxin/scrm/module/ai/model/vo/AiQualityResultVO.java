package com.muxin.scrm.module.ai.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "AI 质检结果")
public class AiQualityResultVO {

    private Long id;
    private Long taskId;
    private Long conversationId;
    private String customerName;
    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private String severity;
    private Integer hit;
    private String evidence;
    private String reason;
    private Integer score;
    private Integer reviewStatus;
    private String reviewNote;
    private Long reviewerId;
    private String reviewerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
