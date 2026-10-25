package com.muxin.scrm.module.ai.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muxin.scrm.core.exception.BusinessException;
import com.muxin.scrm.module.ai.client.AiClient;
import com.muxin.scrm.module.ai.client.AiClientFactory;
import com.muxin.scrm.module.ai.config.AiProperties;
import com.muxin.scrm.module.ai.mapper.AiModelConfigMapper;
import com.muxin.scrm.module.ai.model.bo.AiChatRequest;
import com.muxin.scrm.module.ai.model.bo.AiChatResult;
import com.muxin.scrm.module.ai.model.bo.AiRuntimeConfig;
import com.muxin.scrm.module.ai.model.entity.AiModelConfig;
import com.muxin.scrm.module.ai.model.form.AiModelConfigForm;
import com.muxin.scrm.module.ai.model.vo.AiModelConfigVO;
import com.muxin.scrm.module.ai.model.vo.AiTestResultVO;
import com.muxin.scrm.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiModelConfigService extends ServiceImpl<AiModelConfigMapper, AiModelConfig> {

    private final AiCryptoService cryptoService;
    private final AiProperties properties;
    private final AiClientFactory clientFactory;
    private final AiUsageService usageService;

    public AiModelConfig getDefaultConfig() {
        AiModelConfig config = this.getOne(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getEnabled, 1)
                .orderByDesc(AiModelConfig::getIsDefault)
                .orderByAsc(AiModelConfig::getId)
                .last("LIMIT 1"));
        return config == null ? createMockConfig() : config;
    }

    public AiRuntimeConfig getRuntimeConfig() {
        AiModelConfig config = getDefaultConfig();
        String apiKey = null;
        try {
            apiKey = cryptoService.decrypt(config.getApiKeyCipher());
        } catch (BusinessException ignored) {
            // 加密口令变更后旧密文无法解密，按未配置 Key 处理并回落到 Mock
        }
        return new AiRuntimeConfig(
                StrUtil.blankToDefault(config.getProvider(), "mock"),
                config.getBaseUrl(),
                apiKey,
                StrUtil.blankToDefault(config.getModel(), "muxin-mock-v1"),
                config.getTemperature(),
                config.getMaxTokens(),
                Boolean.TRUE.equals(properties.getFallbackMock())
        );
    }

    public AiModelConfigVO getConfigVO() {
        AiModelConfig config = getDefaultConfig();
        AiModelConfigVO vo = BeanUtil.copyProperties(config, AiModelConfigVO.class);
        boolean hasApiKey = StrUtil.isNotBlank(config.getApiKeyCipher());
        vo.setHasApiKey(hasApiKey);
        if (hasApiKey) {
            try {
                vo.setApiKeyMasked(mask(cryptoService.decrypt(config.getApiKeyCipher())));
            } catch (BusinessException ex) {
                vo.setApiKeyMasked("已配置（密钥不可解密，请重新保存）");
            }
        }
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public AiModelConfigVO saveConfig(AiModelConfigForm form) {
        AiModelConfig config = getDefaultConfig();
        config.setName(form.getName().trim());
        config.setProvider(StrUtil.blankToDefault(form.getProvider(), "mock").trim().toLowerCase());
        config.setBaseUrl(StrUtil.trimToNull(form.getBaseUrl()));
        config.setModel(form.getModel().trim());
        config.setTemperature(form.getTemperature() == null ? new BigDecimal("0.30") : form.getTemperature());
        config.setMaxTokens(form.getMaxTokens() == null ? 1200 : form.getMaxTokens());
        config.setEnabled(form.getEnabled() == null ? 1 : form.getEnabled());
        config.setIsDefault(form.getIsDefault() == null ? 1 : form.getIsDefault());

        String apiKey = form.getApiKey();
        if ("__CLEAR__".equals(apiKey)) {
            config.setApiKeyCipher(null);
        } else if (StrUtil.isNotBlank(apiKey)) {
            config.setApiKeyCipher(cryptoService.encrypt(apiKey.trim()));
        }
        config.setUpdateBy(SecurityUtils.getUserId());
        config.setUpdateTime(LocalDateTime.now());
        this.updateById(config);

        if (Integer.valueOf(1).equals(config.getIsDefault())) {
            this.update(new LambdaUpdateWrapper<AiModelConfig>()
                    .ne(AiModelConfig::getId, config.getId())
                    .set(AiModelConfig::getIsDefault, 0));
        }
        return getConfigVO();
    }

    public AiTestResultVO testConnection() {
        AiRuntimeConfig runtime = getRuntimeConfig();
        AiClient client = clientFactory.get(runtime);
        AiChatRequest request = new AiChatRequest(
                "test",
                "你是企业 AI 助手连通性检测器。",
                "请只回复：木心 MUXIN AI 通道正常。",
                0.1,
                64,
                Map.of()
        );
        long start = System.currentTimeMillis();
        try {
            AiChatResult result = client.chat(request, runtime);
            usageService.recordSuccess("test", result);
            AiTestResultVO vo = new AiTestResultVO();
            vo.setProvider(result.provider());
            vo.setModel(result.model());
            vo.setMocked(!clientFactory.isReal(runtime));
            vo.setLatencyMs(result.latencyMs());
            vo.setMessage(StrUtil.maxLength(result.content(), 200));
            return vo;
        } catch (RuntimeException ex) {
            usageService.recordFailure("test", runtime, (int) (System.currentTimeMillis() - start), ex.getMessage());
            throw ex instanceof BusinessException be ? be : new BusinessException("测试失败：" + ex.getMessage());
        }
    }

    private AiModelConfig createMockConfig() {
        AiModelConfig config = new AiModelConfig();
        config.setName("默认 Mock 模型");
        config.setProvider("mock");
        config.setModel("muxin-mock-v1");
        config.setTemperature(new BigDecimal("0.30"));
        config.setMaxTokens(1200);
        config.setEnabled(1);
        config.setIsDefault(1);
        Long userId = SecurityUtils.getUserId();
        config.setCreateBy(userId);
        config.setUpdateBy(userId);
        this.save(config);
        return config;
    }

    private String mask(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return null;
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
