package com.muxin.scrm.module.ai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.muxin.scrm.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("ai_quality_result")
public class AiQualityResult extends BaseEntity {

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;
}
