package com.muxin.scrm.module.scrm.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesRankVO {
    private Long ownerId;
    private String ownerName;
    private BigDecimal amount;
}
