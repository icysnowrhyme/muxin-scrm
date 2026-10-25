package com.muxin.scrm.module.ai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.muxin.scrm.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ai_analysis")
public class AiAnalysis extends BaseEntity {

    private Long conversationId;
    private Long customerId;
    private String summary;
    private String demandsJson;
    private String objectionsJson;
    private String budget;
    private String decisionMaker;
    private String commitmentsJson;
    private String nextAction;
    private String sentiment;
    private String intentLevel;
    private String tagsJson;
    private String rawOutput;
    private String provider;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
    private Integer followUpAccepted;
    private Long followUpId;
    private Long createBy;
    private Long updateBy;
}
