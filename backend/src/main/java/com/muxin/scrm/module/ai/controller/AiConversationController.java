package com.muxin.scrm.module.ai.controller;

import com.muxin.scrm.core.web.PageResult;
import com.muxin.scrm.core.web.Result;
import com.muxin.scrm.module.ai.model.form.AiAcceptFollowUpForm;
import com.muxin.scrm.module.ai.model.form.AiConversationForm;
import com.muxin.scrm.module.ai.model.query.AiConversationQuery;
import com.muxin.scrm.module.ai.model.vo.AiAnalysisVO;
import com.muxin.scrm.module.ai.model.vo.AiConversationVO;
import com.muxin.scrm.module.ai.service.AiConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "21.木心 AI 工作台")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiConversationController {

    private final AiConversationService conversationService;

    @Operation(summary = "会话分页列表")
    @GetMapping("/conversations")
    public PageResult<AiConversationVO> getPage(AiConversationQuery query) {
        return PageResult.success(conversationService.getPage(query));
    }

    @Operation(summary = "会话详情")
    @GetMapping("/conversations/{id}")
    public Result<AiConversationVO> getDetail(@PathVariable Long id) {
        return Result.success(conversationService.getDetail(id));
    }

    @Operation(summary = "导入会话")
    @PostMapping("/conversations")
    public Result<AiConversationVO> saveConversation(@RequestBody @Valid AiConversationForm form) {
        return Result.success(conversationService.saveConversation(form));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/conversations/{ids}")
    public Result<?> deleteConversations(@PathVariable String ids) {
        return Result.judge(conversationService.deleteConversations(ids));
    }

    @Operation(summary = "生成会话摘要与标签建议")
    @PostMapping("/conversations/{id}/analyze")
    public Result<AiAnalysisVO> analyze(@PathVariable Long id) {
        return Result.success(conversationService.analyze(id));
    }

    @Operation(summary = "获取会话分析结果")
    @GetMapping("/conversations/{id}/analysis")
    public Result<AiAnalysisVO> getAnalysis(@PathVariable Long id) {
        return Result.success(conversationService.getAnalysis(id));
    }

    @Operation(summary = "采纳 AI 摘要为跟进记录")
    @PostMapping("/analyses/{id}/accept-follow-up")
    public Result<Long> acceptFollowUp(@PathVariable Long id,
                                       @RequestBody(required = false) AiAcceptFollowUpForm form) {
        return Result.success(conversationService.acceptFollowUp(id,
                form == null ? new AiAcceptFollowUpForm() : form));
    }

    @Operation(summary = "采纳标签建议")
    @PostMapping("/tag-suggestions/{id}/accept")
    public Result<?> acceptTag(@PathVariable Long id) {
        return Result.judge(conversationService.acceptTag(id));
    }

    @Operation(summary = "忽略标签建议")
    @PostMapping("/tag-suggestions/{id}/reject")
    public Result<?> rejectTag(@PathVariable Long id) {
        return Result.judge(conversationService.rejectTag(id));
    }
}
