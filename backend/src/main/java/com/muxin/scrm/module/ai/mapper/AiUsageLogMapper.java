package com.muxin.scrm.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muxin.scrm.module.ai.model.bo.AiUsageFeatureBO;
import com.muxin.scrm.module.ai.model.entity.AiUsageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiUsageLogMapper extends BaseMapper<AiUsageLog> {

    @Select("""
            SELECT feature,
                   COUNT(*) AS calls,
                   COALESCE(SUM(total_tokens), 0) AS tokens,
                   COALESCE(AVG(latency_ms), 0) AS avgLatency,
                   COALESCE(SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END), 0) AS successCalls
            FROM ai_usage_log
            GROUP BY feature
            ORDER BY calls DESC
            """)
    List<AiUsageFeatureBO> statByFeature();
}
