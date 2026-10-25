package com.muxin.scrm.module.ai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.muxin.scrm.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ai_quality_rule")
public class AiQualityRule extends BaseEntity {

    private String code;
    private String name;
    private String category;
    private String description;
    private String instruction;
    private String severity;
    private Integer enabled;
    private Integer sort;
    private Long createBy;
    private Long updateBy;
    private Integer isDeleted;
}
