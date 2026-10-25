package com.muxin.scrm.module.ai.model.query;

import com.muxin.scrm.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "AI 质检任务查询")
public class AiQualityTaskQuery extends BaseQuery {

    private String keyword;
    private Integer status;
}
