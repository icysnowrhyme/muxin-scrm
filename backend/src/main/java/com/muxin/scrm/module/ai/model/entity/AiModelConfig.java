package com.muxin.scrm.module.ai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.muxin.scrm.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("ai_model_config")
public class AiModelConfig extends BaseEntity {

    private String name;
    private String provider;
    private String baseUrl;
    private String apiKeyCipher;
    private String model;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer enabled;
    private Integer isDefault;
    private Long createBy;
    private Long updateBy;
}
