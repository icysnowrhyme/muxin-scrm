package com.muxin.scrm.module.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI 质检概览")
public class AiQualityStatsVO {

    private Long ruleCount;
    private Long enabledRuleCount;
    private Long taskCount;
    private Long runningTaskCount;
    private Long hitCount;
    private Long pendingReview;
    private Long confirmedCount;
    private Long falsePositiveCount;
}
