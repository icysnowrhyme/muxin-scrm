package com.muxin.scrm.module.scrm.model.query;

import com.muxin.scrm.common.base.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CampaignQuery extends BaseQuery {

    private String keyword;
    private Integer type;
    private Integer status;
}
