package com.muxin.scrm.module.ai.model.query;

import com.muxin.scrm.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "AI 质检结果查询")
public class AiQualityResultQuery extends BaseQuery {

    private Long taskId;
    private Long conversationId;
    private String severity;
    private Integer reviewStatus;

    @Schema(description = "是否只看命中结果")
    private Boolean hitOnly;
}
