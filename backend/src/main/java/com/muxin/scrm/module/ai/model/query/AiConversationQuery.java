package com.muxin.scrm.module.ai.model.query;

import com.muxin.scrm.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "AI 会话查询")
public class AiConversationQuery extends BaseQuery {

    private String keyword;
    private Long customerId;

    @Schema(description = "分析状态：0 未分析，1 已分析")
    private Integer status;
}
