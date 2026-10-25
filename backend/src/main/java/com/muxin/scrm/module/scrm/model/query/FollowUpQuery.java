package com.muxin.scrm.module.scrm.model.query;

import com.muxin.scrm.common.base.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class FollowUpQuery extends BaseQuery {

    private Long customerId;
    private String keyword;
    private Integer type;
    private Long creatorId;
    private String startDate;
    private String endDate;

    /** 仅查询当前用户的客户及跟进记录，由服务端根据数据权限设置 */
    private Long scopeOwnerId;
}
