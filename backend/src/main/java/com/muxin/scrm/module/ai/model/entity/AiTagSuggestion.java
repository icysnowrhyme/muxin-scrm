package com.muxin.scrm.module.ai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.muxin.scrm.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("ai_tag_suggestion")
public class AiTagSuggestion extends BaseEntity {

    private Long conversationId;
    private Long analysisId;
    private Long customerId;
    private Long tagId;
    private String tagName;
    private BigDecimal confidence;
    private String evidence;
    private Integer status;
    private Long createBy;
}
