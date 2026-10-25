package com.muxin.scrm.module.ai.controller;

import com.muxin.scrm.core.web.Result;
import com.muxin.scrm.module.ai.model.form.AiModelConfigForm;
import com.muxin.scrm.module.ai.model.form.AiPromptTemplateForm;
import com.muxin.scrm.module.ai.model.entity.AiPromptTemplate;
import com.muxin.scrm.module.ai.model.vo.AiModelConfigVO;
import com.muxin.scrm.module.ai.model.vo.AiTestResultVO;
import com.muxin.scrm.module.ai.model.vo.AiUsageSummaryVO;
import com.muxin.scrm.module.ai.service.AiModelConfigService;
import com.muxin.scrm.module.ai.service.AiPromptTemplateService;
import com.muxin.scrm.module.ai.service.AiUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "23.木心 AI 设置")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiSettingsController {

    private final AiModelConfigService modelConfigService;
    private final AiPromptTemplateService promptTemplateService;
    private final AiUsageService usageService;

    @Operation(summary = "获取模型配置")
    @GetMapping("/config")
    @PreAuthorize("@ss.hasPerm('ai:config:list')")
    public Result<AiModelConfigVO> getConfig() {
        return Result.success(modelConfigService.getConfigVO());
    }

    @Operation(summary = "保存模型配置")
    @PutMapping("/config")
    @PreAuthorize("@ss.hasPerm('ai:config:update')")
    public Result<AiModelConfigVO> saveConfig(@RequestBody @Valid AiModelConfigForm form) {
        return Result.success(modelConfigService.saveConfig(form));
    }

    @Operation(summary = "测试模型连通性")
    @PostMapping("/config/test")
    @PreAuthorize("@ss.hasPerm('ai:config:test')")
    public Result<AiTestResultVO> testConnection() {
        return Result.success(modelConfigService.testConnection());
    }

    @Operation(summary = "AI 用量概览")
    @GetMapping("/usage/summary")
    @PreAuthorize("@ss.hasPerm('ai:config:list')")
    public Result<AiUsageSummaryVO> usageSummary() {
        return Result.success(usageService.summary());
    }

    @Operation(summary = "Prompt 模板列表")
    @GetMapping("/prompts")
    @PreAuthorize("@ss.hasPerm('ai:config:list')")
    public Result<List<AiPromptTemplate>> listPrompts() {
        return Result.success(promptTemplateService.listTemplates());
    }

    @Operation(summary = "保存 Prompt 模板")
    @PutMapping("/prompts/{code}")
    @PreAuthorize("@ss.hasPerm('ai:config:update')")
    public Result<AiPromptTemplate> savePrompt(@PathVariable String code,
                                               @RequestBody @Valid AiPromptTemplateForm form) {
        return Result.success(promptTemplateService.updateTemplate(code, form));
    }
}
