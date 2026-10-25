package com.muxin.scrm.module.ai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.muxin.scrm.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ai_quality_task")
public class AiQualityTask extends BaseEntity {

    private String name;
    private String conversationIds;
    private String ruleIds;
    private Integer totalCount;
    private Integer processedCount;
    private Integer hitCount;
    private Integer status;
    private String errorMessage;
    private Long createBy;
}
