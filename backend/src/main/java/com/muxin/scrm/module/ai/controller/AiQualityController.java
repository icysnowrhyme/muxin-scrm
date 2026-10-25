package com.muxin.scrm.module.ai.controller;

import com.muxin.scrm.core.web.PageResult;
import com.muxin.scrm.core.web.Result;
import com.muxin.scrm.module.ai.model.entity.AiQualityRule;
import com.muxin.scrm.module.ai.model.entity.AiQualityTask;
import com.muxin.scrm.module.ai.model.form.AiQualityReviewForm;
import com.muxin.scrm.module.ai.model.form.AiQualityRuleForm;
import com.muxin.scrm.module.ai.model.form.AiQualityTaskForm;
import com.muxin.scrm.module.ai.model.query.AiQualityResultQuery;
import com.muxin.scrm.module.ai.model.query.AiQualityTaskQuery;
import com.muxin.scrm.module.ai.model.vo.AiQualityResultVO;
import com.muxin.scrm.module.ai.model.vo.AiQualityStatsVO;
import com.muxin.scrm.module.ai.service.AiQualityRuleService;
import com.muxin.scrm.module.ai.service.AiQualityTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "22.木心 AI 质检")
@RestController
@RequestMapping("/api/v1/ai/quality")
@RequiredArgsConstructor
public class AiQualityController {

    private final AiQualityRuleService ruleService;
    private final AiQualityTaskService taskService;

    @Operation(summary = "质检概览")
    @GetMapping("/stats")
    public Result<AiQualityStatsVO> stats() {
        return Result.success(taskService.stats());
    }

    @Operation(summary = "质检规则列表")
    @GetMapping("/rules")
    public Result<List<AiQualityRule>> listRules(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer enabled) {
        return Result.success(ruleService.listRules(keyword, enabled));
    }

    @Operation(summary = "新增质检规则")
    @PostMapping("/rules")
    @PreAuthorize("@ss.hasPerm('ai:quality:rule')")
    public Result<?> saveRule(@RequestBody @Valid AiQualityRuleForm form) {
        return Result.judge(ruleService.saveRule(form));
    }

    @Operation(summary = "修改质检规则")
    @PutMapping("/rules/{id}")
    @PreAuthorize("@ss.hasPerm('ai:quality:rule')")
    public Result<?> updateRule(@PathVariable Long id, @RequestBody @Valid AiQualityRuleForm form) {
        return Result.judge(ruleService.updateRule(id, form));
    }

    @Operation(summary = "删除质检规则")
    @DeleteMapping("/rules/{ids}")
    @PreAuthorize("@ss.hasPerm('ai:quality:rule')")
    public Result<?> deleteRules(@PathVariable String ids) {
        return Result.judge(ruleService.deleteRules(ids));
    }

    @Operation(summary = "质检任务分页列表")
    @GetMapping("/tasks")
    public PageResult<AiQualityTask> getTaskPage(AiQualityTaskQuery query) {
        return PageResult.success(taskService.getPage(query));
    }

    @Operation(summary = "创建质检任务")
    @PostMapping("/tasks")
    public Result<AiQualityTask> startTask(@RequestBody @Valid AiQualityTaskForm form) {
        return Result.success(taskService.startTask(form));
    }

    @Operation(summary = "质检结果分页列表")
    @GetMapping("/results")
    public PageResult<AiQualityResultVO> getResultPage(AiQualityResultQuery query) {
        return PageResult.success(taskService.getResultPage(query));
    }

    @Operation(summary = "复核质检结果")
    @PutMapping("/results/{id}/review")
    public Result<?> review(@PathVariable Long id, @RequestBody @Valid AiQualityReviewForm form) {
        return Result.judge(taskService.review(id, form));
    }
}
