package com.muxin.scrm.module.ai.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muxin.scrm.core.exception.BusinessException;
import com.muxin.scrm.module.ai.mapper.AiConversationMapper;
import com.muxin.scrm.module.ai.mapper.AiQualityResultMapper;
import com.muxin.scrm.module.ai.mapper.AiQualityTaskMapper;
import com.muxin.scrm.module.ai.model.bo.AiChatRequest;
import com.muxin.scrm.module.ai.model.bo.AiChatResult;
import com.muxin.scrm.module.ai.model.bo.AiPromptPair;
import com.muxin.scrm.module.ai.model.entity.AiConversation;
import com.muxin.scrm.module.ai.model.entity.AiQualityResult;
import com.muxin.scrm.module.ai.model.entity.AiQualityRule;
import com.muxin.scrm.module.ai.model.entity.AiQualityTask;
import com.muxin.scrm.module.ai.model.form.AiQualityReviewForm;
import com.muxin.scrm.module.ai.model.form.AiQualityTaskForm;
import com.muxin.scrm.module.ai.model.query.AiQualityResultQuery;
import com.muxin.scrm.module.ai.model.query.AiQualityTaskQuery;
import com.muxin.scrm.module.ai.model.vo.AiQualityResultVO;
import com.muxin.scrm.module.ai.model.vo.AiQualityStatsVO;
import com.muxin.scrm.module.scrm.support.ScrmDataScope;
import com.muxin.scrm.security.util.SecurityUtils;
import com.muxin.scrm.system.model.entity.User;
import com.muxin.scrm.system.service.UserService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiQualityTaskService extends ServiceImpl<AiQualityTaskMapper, AiQualityTask> {

    private final AiQualityResultMapper resultMapper;
    private final AiConversationMapper conversationMapper;
    private final AiConversationService conversationService;
    private final AiQualityRuleService ruleService;
    private final AiGatewayService gatewayService;
    private final AiPromptTemplateService promptService;
    private final AiJsonService jsonService;
    private final UserService userService;

    @Resource(name = "aiTaskExecutor")
    private ThreadPoolTaskExecutor aiTaskExecutor;

    public IPage<AiQualityTask> getPage(AiQualityTaskQuery query) {
        LambdaQueryWrapper<AiQualityTask> wrapper = new LambdaQueryWrapper<>();
        Long scope = ScrmDataScope.scopeOwnerId();
        wrapper.eq(scope != null, AiQualityTask::getCreateBy, scope)
                .eq(query.getStatus() != null, AiQualityTask::getStatus, query.getStatus());
        if (StrUtil.isNotBlank(query.getKeyword())) {
            wrapper.like(AiQualityTask::getName, query.getKeyword().trim());
        }
        wrapper.orderByDesc(AiQualityTask::getId);
        return this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    public AiQualityTask startTask(AiQualityTaskForm form) {
        List<Long> conversationIds = form.getConversationIds().stream().filter(Objects::nonNull).distinct().toList();
        if (conversationIds.isEmpty()) {
            throw new BusinessException("请选择要质检的会话");
        }
        List<AiConversation> conversations = conversationIds.stream().map(id -> {
            conversationService.getDetail(id);
            return conversationMapper.selectById(id);
        }).filter(Objects::nonNull).toList();
        if (conversations.isEmpty()) {
            throw new BusinessException("会话不存在");
        }
        List<AiQualityRule> rules = ruleService.listEnabledRules(form.getRuleIds());
        if (rules.isEmpty()) {
            throw new BusinessException("没有可用的质检规则");
        }

        AiQualityTask task = new AiQualityTask();
        task.setName(StrUtil.blankToDefault(form.getName(), "质检任务 "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))));
        task.setConversationIds(conversationIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        task.setRuleIds(rules.stream().map(rule -> String.valueOf(rule.getId())).collect(Collectors.joining(",")));
        task.setTotalCount(conversations.size());
        task.setProcessedCount(0);
        task.setHitCount(0);
        task.setStatus(0);
        task.setCreateBy(SecurityUtils.getUserId());
        this.save(task);

        Long taskId = task.getId();
        aiTaskExecutor.execute(() -> runTask(taskId, conversations, rules));
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean review(Long id, AiQualityReviewForm form) {
        if (!ScrmDataScope.canViewAll()) {
            throw new BusinessException("仅管理角色可复核质检结果");
        }
        if (!Integer.valueOf(1).equals(form.getReviewStatus()) && !Integer.valueOf(2).equals(form.getReviewStatus())) {
            throw new BusinessException("复核结果只能为 1（确认违规）或 2（误报）");
        }
        AiQualityResult result = resultMapper.selectById(id);
        if (result == null) {
            throw new BusinessException("质检结果不存在");
        }
        if (!Integer.valueOf(1).equals(result.getHit())) {
            throw new BusinessException("仅命中项需要复核");
        }
        result.setReviewStatus(form.getReviewStatus());
        result.setReviewNote(StrUtil.trimToNull(form.getReviewNote()));
        result.setReviewerId(SecurityUtils.getUserId());
        result.setReviewTime(LocalDateTime.now());
        return resultMapper.updateById(result) > 0;
    }

    public IPage<AiQualityResultVO> getResultPage(AiQualityResultQuery query) {
        LambdaQueryWrapper<AiQualityResult> wrapper = new LambdaQueryWrapper<>();
        Long scope = ScrmDataScope.scopeOwnerId();
        if (scope != null) {
            wrapper.inSql(AiQualityResult::getTaskId,
                    "SELECT id FROM ai_quality_task WHERE create_by = " + scope);
        }
        wrapper.eq(query.getTaskId() != null, AiQualityResult::getTaskId, query.getTaskId())
                .eq(query.getConversationId() != null, AiQualityResult::getConversationId, query.getConversationId())
                .eq(StrUtil.isNotBlank(query.getSeverity()), AiQualityResult::getSeverity, query.getSeverity())
                .eq(query.getReviewStatus() != null, AiQualityResult::getReviewStatus, query.getReviewStatus())
                .eq(Boolean.TRUE.equals(query.getHitOnly()), AiQualityResult::getHit, 1)
                .orderByDesc(AiQualityResult::getCreateTime)
                .orderByDesc(AiQualityResult::getId);
        Page<AiQualityResult> page = resultMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<AiQualityResult> records = page.getRecords();
        Set<Long> reviewerIds = records.stream().map(AiQualityResult::getReviewerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> reviewerNames = reviewerIds.isEmpty() ? Map.of()
                : userService.listByIds(reviewerIds).stream().collect(Collectors.toMap(User::getId,
                user -> StrUtil.blankToDefault(user.getNickname(), user.getUsername())));

        Page<AiQualityResultVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records.stream().map(item -> {
            AiQualityResultVO vo = BeanUtil.copyProperties(item, AiQualityResultVO.class);
            vo.setReviewerName(item.getReviewerId() == null ? null : reviewerNames.get(item.getReviewerId()));
            return vo;
        }).toList());
        return voPage;
    }

    public AiQualityStatsVO stats() {
        AiQualityStatsVO vo = new AiQualityStatsVO();
        vo.setRuleCount(ruleService.count());
        vo.setEnabledRuleCount(ruleService.count(new LambdaQueryWrapper<AiQualityRule>()
                .eq(AiQualityRule::getEnabled, 1)));
        vo.setTaskCount(this.count());
        vo.setRunningTaskCount(this.count(new LambdaQueryWrapper<AiQualityTask>()
                .in(AiQualityTask::getStatus, 0, 1)));
        vo.setHitCount(resultMapper.selectCount(new LambdaQueryWrapper<AiQualityResult>()
                .eq(AiQualityResult::getHit, 1)));
        vo.setPendingReview(resultMapper.selectCount(new LambdaQueryWrapper<AiQualityResult>()
                .eq(AiQualityResult::getHit, 1).eq(AiQualityResult::getReviewStatus, 0)));
        vo.setConfirmedCount(resultMapper.selectCount(new LambdaQueryWrapper<AiQualityResult>()
                .eq(AiQualityResult::getReviewStatus, 1)));
        vo.setFalsePositiveCount(resultMapper.selectCount(new LambdaQueryWrapper<AiQualityResult>()
                .eq(AiQualityResult::getReviewStatus, 2)));
        return vo;
    }

    private void runTask(Long taskId, List<AiConversation> conversations, List<AiQualityRule> rules) {
        updateTask(taskId, 1, 0, 0, null);
        int processed = 0;
        int hits = 0;
        try {
            for (AiConversation conversation : conversations) {
                hits += processConversation(taskId, conversation, rules);
                processed++;
                updateTask(taskId, 1, processed, hits, null);
            }
            updateTask(taskId, 2, processed, hits, null);
        } catch (Exception ex) {
            log.warn("AI 质检任务执行失败：taskId={}", taskId, ex);
            updateTask(taskId, 3, processed, hits, StrUtil.maxLength(ex.getMessage(), 900));
        }
    }

    private int processConversation(Long taskId, AiConversation conversation, List<AiQualityRule> rules) {
        AiPromptPair pair = promptService.getPair("quality");
        String rulesText = rules.stream()
                .map(rule -> "- [" + rule.getCode() + "] " + rule.getName() + "：" + rule.getInstruction()
                        + "（严重级别：" + rule.getSeverity() + "）")
                .collect(Collectors.joining("\n"));
        String userPrompt = promptService.render(pair.userTemplate(), Map.of(
                "rules", rulesText,
                "conversation", conversation.getContent()
        ));
        List<Map<String, Object>> ruleContext = rules.stream().map(rule -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", rule.getCode());
            map.put("name", rule.getName());
            map.put("severity", rule.getSeverity());
            return map;
        }).toList();
        AiChatResult chatResult = gatewayService.chat(new AiChatRequest(
                "quality",
                pair.systemPrompt(),
                userPrompt,
                0.1,
                1500,
                Map.of("rules", ruleContext, "conversation", conversation.getContent())
        ));
        JsonNode root = jsonService.readObject(chatResult.content());
        Map<String, JsonNode> violations = new HashMap<>();
        JsonNode violationNodes = root.path("violations");
        if (violationNodes.isArray()) {
            violationNodes.forEach(node -> {
                String code = node.path("ruleCode").asText("").toUpperCase();
                if (StrUtil.isNotBlank(code)) {
                    violations.put(code, node);
                }
            });
        }

        int hits = 0;
        for (AiQualityRule rule : rules) {
            JsonNode violation = violations.get(rule.getCode().toUpperCase());
            boolean hit = violation != null;
            AiQualityResult result = new AiQualityResult();
            result.setTaskId(taskId);
            result.setConversationId(conversation.getId());
            result.setCustomerName(conversation.getCustomerName());
            result.setRuleId(rule.getId());
            result.setRuleCode(rule.getCode());
            result.setRuleName(rule.getName());
            result.setSeverity(rule.getSeverity());
            result.setHit(hit ? 1 : 0);
            if (hit) {
                result.setEvidence(StrUtil.maxLength(StrUtil.blankToDefault(
                        violation.path("evidence").asText(null), "模型未给出原文片段"), 900));
                result.setReason(StrUtil.maxLength(StrUtil.blankToDefault(
                        violation.path("reason").asText(null), rule.getInstruction()), 900));
                result.setScore(scoreOf(rule.getSeverity()));
                hits++;
            } else {
                result.setScore(100);
            }
            result.setReviewStatus(0);
            resultMapper.insert(result);
        }
        return hits;
    }

    private void updateTask(Long id, int status, int processed, int hits, String errorMessage) {
        this.update(new LambdaUpdateWrapper<AiQualityTask>()
                .eq(AiQualityTask::getId, id)
                .set(AiQualityTask::getStatus, status)
                .set(AiQualityTask::getProcessedCount, processed)
                .set(AiQualityTask::getHitCount, hits)
                .set(AiQualityTask::getErrorMessage, errorMessage)
                .set(AiQualityTask::getUpdateTime, LocalDateTime.now()));
    }

    private int scoreOf(String severity) {
        return switch (StrUtil.blankToDefault(severity, "medium")) {
            case "high" -> 40;
            case "medium" -> 70;
            default -> 90;
        };
    }
}
