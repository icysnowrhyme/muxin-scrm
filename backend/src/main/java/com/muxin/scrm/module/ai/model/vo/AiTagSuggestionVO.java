package com.muxin.scrm.module.ai.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiTagSuggestionVO {

    private Long id;
    private Long conversationId;
    private Long analysisId;
    private Long customerId;
    private Long tagId;
    private String tagName;
    private BigDecimal confidence;
    private String evidence;
    private Integer status;
}
