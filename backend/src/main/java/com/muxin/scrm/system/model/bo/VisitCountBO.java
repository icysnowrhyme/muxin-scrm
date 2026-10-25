package com.muxin.scrm.system.model.bo;

import lombok.Data;

/**
 * 特定日期访问统计
 *
 * @author MUXIN Team
 * @since 2.10.0
 */
@Data
public class VisitCountBO {

    /**
     * 日期 yyyy-MM-dd
     */
    private String date;

    /**
     * 访问次数
     */
    private Integer count;
}
