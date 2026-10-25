package com.muxin.scrm.module.ai.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muxin.scrm.module.ai.mapper.AiUsageLogMapper;
import com.muxin.scrm.module.ai.model.bo.AiChatResult;
import com.muxin.scrm.module.ai.model.bo.AiRuntimeConfig;
import com.muxin.scrm.module.ai.model.bo.AiUsageFeatureBO;
import com.muxin.scrm.module.ai.model.entity.AiUsageLog;
import com.muxin.scrm.module.ai.model.vo.AiUsageSummaryVO;
import com.muxin.scrm.security.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiUsageService extends ServiceImpl<AiUsageLogMapper, AiUsageLog> {

    public void recordSuccess(String feature, AiChatResult result) {
        saveLog(feature, result.provider(), result.model(), result.promptTokens(),
                result.completionTokens(), result.totalTokens(), result.latencyMs(), 1, null);
    }

    public void recordFailure(String feature, AiRuntimeConfig config, int latencyMs, String message) {
        saveLog(feature, config.provider(), config.model(), 0, 0, 0, latencyMs, 0,
                StrUtil.maxLength(StrUtil.blankToDefault(message, "未知错误"), 900));
    }

    public AiUsageSummaryVO summary() {
        List<AiUsageFeatureBO> features = baseMapper.statByFeature();
        long totalCalls = 0;
        long totalTokens = 0;
        long successCalls = 0;
        long weightedLatency = 0;
        for (AiUsageFeatureBO feature : features) {
            long calls = feature.getCalls() == null ? 0 : feature.getCalls();
            totalCalls += calls;
            totalTokens += feature.getTokens() == null ? 0 : feature.getTokens();
            successCalls += feature.getSuccessCalls() == null ? 0 : feature.getSuccessCalls();
            long avgLatency = feature.getAvgLatency() == null ? 0 : feature.getAvgLatency().longValue();
            weightedLatency += avgLatency * calls;
        }
        AiUsageSummaryVO vo = new AiUsageSummaryVO();
        vo.setTotalCalls(totalCalls);
        vo.setTotalTokens(totalTokens);
        vo.setSuccessCalls(successCalls);
        vo.setSuccessRate(totalCalls == 0 ? 0 : (int) Math.round(successCalls * 100.0 / totalCalls));
        vo.setAvgLatency(totalCalls == 0 ? 0 : weightedLatency / totalCalls);
        vo.setFeatures(features);
        return vo;
    }

    private void saveLog(String feature, String provider, String model, int promptTokens,
                         int completionTokens, int totalTokens, int latencyMs,
                         int success, String errorMessage) {
        AiUsageLog log = new AiUsageLog();
        log.setFeature(feature);
        log.setProvider(provider);
        log.setModel(model);
        log.setPromptTokens(promptTokens);
        log.setCompletionTokens(completionTokens);
        log.setTotalTokens(totalTokens);
        log.setLatencyMs(latencyMs);
        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        log.setCreateBy(SecurityUtils.getUserId());
        this.save(log);
    }
}
