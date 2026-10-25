package com.muxin.scrm.module.ai.model.bo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiUsageFeatureBO {

    private String feature;
    private Long calls;
    private Long tokens;
    private BigDecimal avgLatency;
    private Long successCalls;
}
