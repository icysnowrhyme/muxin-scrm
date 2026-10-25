package com.muxin.scrm.module.ai.model.vo;

import com.muxin.scrm.module.ai.model.bo.AiUsageFeatureBO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI 用量概览")
public class AiUsageSummaryVO {

    private Long totalCalls;
    private Long totalTokens;
    private Long successCalls;

    @Schema(description = "成功率，百分比整数")
    private Integer successRate;

    private Long avgLatency;
    private List<AiUsageFeatureBO> features;
}
