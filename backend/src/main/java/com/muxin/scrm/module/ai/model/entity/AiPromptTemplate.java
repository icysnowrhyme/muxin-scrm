package com.muxin.scrm.module.ai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.muxin.scrm.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ai_prompt_template")
public class AiPromptTemplate extends BaseEntity {

    private String code;
    private String name;
    private String systemPrompt;
    private String userTemplate;
    private Integer version;
    private Integer enabled;
    private Long createBy;
    private Long updateBy;
}
