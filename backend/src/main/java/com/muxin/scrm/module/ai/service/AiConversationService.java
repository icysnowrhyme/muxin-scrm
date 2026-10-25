package com.muxin.scrm.module.ai.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muxin.scrm.core.exception.BusinessException;
import com.muxin.scrm.module.ai.mapper.AiAnalysisMapper;
import com.muxin.scrm.module.ai.mapper.AiConversationMapper;
import com.muxin.scrm.module.ai.mapper.AiTagSuggestionMapper;
import com.muxin.scrm.module.ai.model.bo.AiChatRequest;
import com.muxin.scrm.module.ai.model.bo.AiChatResult;
import com.muxin.scrm.module.ai.model.bo.AiPromptPair;
import com.muxin.scrm.module.ai.model.entity.AiAnalysis;
import com.muxin.scrm.module.ai.model.entity.AiConversation;
import com.muxin.scrm.module.ai.model.entity.AiTagSuggestion;
import com.muxin.scrm.module.ai.model.form.AiAcceptFollowUpForm;
import com.muxin.scrm.module.ai.model.form.AiConversationForm;
import com.muxin.scrm.module.ai.model.query.AiConversationQuery;
import com.muxin.scrm.module.ai.model.vo.AiAnalysisVO;
import com.muxin.scrm.module.ai.model.vo.AiConversationVO;
import com.muxin.scrm.module.ai.model.vo.AiTagSuggestionVO;
import com.muxin.scrm.module.scrm.mapper.CustomerTagMapper;
import com.muxin.scrm.module.scrm.mapper.CustomerTagRelMapper;
import com.muxin.scrm.module.scrm.model.entity.CustomerTag;
import com.muxin.scrm.module.scrm.model.entity.CustomerTagRel;
import com.muxin.scrm.module.scrm.model.entity.FollowUp;
import com.muxin.scrm.module.scrm.model.form.FollowUpForm;
import com.muxin.scrm.module.scrm.model.vo.CustomerDetailVO;
import com.muxin.scrm.module.scrm.service.CustomerService;
import com.muxin.scrm.module.scrm.service.FollowUpService;
import com.muxin.scrm.module.scrm.support.ScrmDataScope;
import com.muxin.scrm.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AiConversationService extends ServiceImpl<AiConversationMapper, AiConversation> {

    private static final String[] TAG_COLORS = {
            "#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#909399", "#7B61FF", "#00B8A9"
    };

    private final AiAnalysisMapper analysisMapper;
    private final AiTagSuggestionMapper tagSuggestionMapper;
    private final CustomerTagMapper customerTagMapper;
    private final CustomerTagRelMapper tagRelMapper;
    private final CustomerService customerService;
    private final FollowUpService followUpService;
    private final AiGatewayService gatewayService;
    private final AiPromptTemplateService promptService;
    private final AiJsonService jsonService;

    public IPage<AiConversationVO> getPage(AiConversationQuery query) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        Long scope = ScrmDataScope.scopeOwnerId();
        wrapper.eq(scope != null, AiConversation::getOwnerId, scope)
                .eq(query.getCustomerId() != null, AiConversation::getCustomerId, query.getCustomerId())
                .eq(query.getStatus() != null, AiConversation::getStatus, query.getStatus());
        if (StrUtil.isNotBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(AiConversation::getTitle, keyword)
                    .or().like(AiConversation::getCustomerName, keyword)
                    .or().like(AiConversation::getEmployeeName, keyword));
        }
        wrapper.orderByDesc(AiConversation::getStartedAt).orderByDesc(AiConversation::getId);
        Page<AiConversation> page = this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        Page<AiConversationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(item -> toVO(item, false)).toList());
        return voPage;
    }

    public AiConversationVO getDetail(Long id) {
        return toVO(getAccessibleConversation(id), true);
    }

    @Transactional(rollbackFor = Exception.class)
    public AiConversationVO saveConversation(AiConversationForm form) {
        String customerName = form.getCustomerName();
        if (form.getCustomerId() != null) {
            CustomerDetailVO customer = customerService.getDetail(form.getCustomerId());
            if (StrUtil.isBlank(customerName)) {
                customerName = customer.getName();
            }
        }
        Long userId = SecurityUtils.getUserId();
        AiConversation conversation = new AiConversation();
        conversation.setTitle(StrUtil.blankToDefault(form.getTitle(),
                StrUtil.blankToDefault(customerName, "客户") + "会话 "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))));
        conversation.setCustomerId(form.getCustomerId());
        conversation.setCustomerName(customerName);
        conversation.setEmployeeName(form.getEmployeeName());
        conversation.setOwnerId(userId);
        conversation.setSource(StrUtil.blankToDefault(form.getSource(), "import"));
        conversation.setContent(form.getContent());
        conversation.setMessageCount((int) Arrays.stream(form.getContent().split("\n"))
                .filter(StrUtil::isNotBlank).count());
        conversation.setStartedAt(form.getStartedAt() == null ? LocalDateTime.now() : form.getStartedAt());
        conversation.setStatus(0);
        conversation.setCreateBy(userId);
        conversation.setUpdateBy(userId);
        this.save(conversation);
        return toVO(conversation, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConversations(String ids) {
        List<Long> idList = StrUtil.split(ids, ',').stream().filter(StrUtil::isNotBlank)
                .map(item -> Long.valueOf(item.trim())).toList();
        if (CollUtil.isEmpty(idList)) {
            return false;
        }
        idList.forEach(this::getAccessibleConversation);
        analysisMapper.delete(new LambdaQueryWrapper<AiAnalysis>().in(AiAnalysis::getConversationId, idList));
        tagSuggestionMapper.delete(new LambdaQueryWrapper<AiTagSuggestion>()
                .in(AiTagSuggestion::getConversationId, idList));
        return this.removeByIds(idList);
    }

    @Transactional(rollbackFor = Exception.class)
    public AiAnalysisVO analyze(Long conversationId) {
        AiConversation conversation = getAccessibleConversation(conversationId);
        String customerName = StrUtil.blankToDefault(conversation.getCustomerName(), "客户");
        AiPromptPair pair = promptService.getPair("summary");
        String userPrompt = promptService.render(pair.userTemplate(), Map.of(
                "customer", customerName,
                "conversation", conversation.getContent()
        ));
        AiChatRequest request = new AiChatRequest(
                "summary",
                pair.systemPrompt(),
                userPrompt,
                0.3,
                1200,
                Map.of("customer", customerName, "conversation", conversation.getContent())
        );
        AiChatResult result = gatewayService.chat(request);
        JsonNode root = jsonService.readObject(result.content());

        List<String> demands = jsonService.stringList(root, "demands");
        List<String> objections = jsonService.stringList(root, "objections");
        List<String> commitments = jsonService.stringList(root, "commitments");
        List<String> tags = jsonService.stringList(root, "tags");

        analysisMapper.delete(new LambdaQueryWrapper<AiAnalysis>()
                .eq(AiAnalysis::getConversationId, conversationId));
        tagSuggestionMapper.delete(new LambdaQueryWrapper<AiTagSuggestion>()
                .eq(AiTagSuggestion::getConversationId, conversationId));

        Long userId = SecurityUtils.getUserId();
        AiAnalysis analysis = new AiAnalysis();
        analysis.setConversationId(conversationId);
        analysis.setCustomerId(conversation.getCustomerId());
        analysis.setSummary(StrUtil.maxLength(StrUtil.blankToDefault(jsonService.text(root, "summary"), "会话已完成分析"), 900));
        analysis.setDemandsJson(jsonService.write(demands));
        analysis.setObjectionsJson(jsonService.write(objections));
        analysis.setBudget(jsonService.text(root, "budget"));
        analysis.setDecisionMaker(jsonService.text(root, "decisionMaker"));
        analysis.setCommitmentsJson(jsonService.write(commitments));
        analysis.setNextAction(jsonService.text(root, "nextAction"));
        analysis.setSentiment(jsonService.text(root, "sentiment"));
        analysis.setIntentLevel(jsonService.text(root, "intentLevel"));
        analysis.setTagsJson(jsonService.write(tags));
        analysis.setRawOutput(result.content());
        analysis.setProvider(result.provider());
        analysis.setModel(result.model());
        analysis.setPromptTokens(result.promptTokens());
        analysis.setCompletionTokens(result.completionTokens());
        analysis.setTotalTokens(result.totalTokens());
        analysis.setLatencyMs(result.latencyMs());
        analysis.setFollowUpAccepted(0);
        analysis.setCreateBy(userId);
        analysis.setUpdateBy(userId);
        analysisMapper.insert(analysis);

        List<String> tagEvidencePool = new ArrayList<>(demands);
        tagEvidencePool.addAll(objections);
        tagEvidencePool.addAll(commitments);
        if (StrUtil.isNotBlank(analysis.getNextAction())) {
            tagEvidencePool.add(analysis.getNextAction());
        }

        int index = 0;
        for (String tagName : tags) {
            if (StrUtil.isBlank(tagName) || index >= 8) {
                continue;
            }
            AiTagSuggestion suggestion = new AiTagSuggestion();
            suggestion.setConversationId(conversationId);
            suggestion.setAnalysisId(analysis.getId());
            suggestion.setCustomerId(conversation.getCustomerId());
            suggestion.setTagId(existingTagId(tagName));
            suggestion.setTagName(StrUtil.maxLength(tagName.trim(), 60));
            suggestion.setConfidence(BigDecimal.valueOf(Math.max(60, 92 - index * 4))
                    .setScale(2, RoundingMode.HALF_UP));
            suggestion.setEvidence(tagEvidence(tagName, tagEvidencePool));
            suggestion.setStatus(0);
            suggestion.setCreateBy(userId);
            tagSuggestionMapper.insert(suggestion);
            index++;
        }

        AiConversation update = new AiConversation();
        update.setId(conversationId);
        update.setStatus(1);
        update.setUpdateBy(userId);
        this.updateById(update);

        return getAnalysis(conversationId);
    }

    public AiAnalysisVO getAnalysis(Long conversationId) {
        getAccessibleConversation(conversationId);
        AiAnalysis analysis = analysisMapper.selectOne(new LambdaQueryWrapper<AiAnalysis>()
                .eq(AiAnalysis::getConversationId, conversationId)
                .orderByDesc(AiAnalysis::getId)
                .last("LIMIT 1"));
        if (analysis == null) {
            return null;
        }
        AiAnalysisVO vo = BeanUtil.copyProperties(analysis, AiAnalysisVO.class);
        vo.setDemands(jsonService.parseStringList(analysis.getDemandsJson()));
        vo.setObjections(jsonService.parseStringList(analysis.getObjectionsJson()));
        vo.setCommitments(jsonService.parseStringList(analysis.getCommitmentsJson()));
        vo.setTags(jsonService.parseStringList(analysis.getTagsJson()));
        List<AiTagSuggestion> suggestions = tagSuggestionMapper.selectList(
                new LambdaQueryWrapper<AiTagSuggestion>()
                        .eq(AiTagSuggestion::getAnalysisId, analysis.getId())
                        .orderByDesc(AiTagSuggestion::getConfidence)
                        .orderByAsc(AiTagSuggestion::getId));
        vo.setSuggestions(suggestions.stream().map(item -> {
            AiTagSuggestionVO suggestionVO = BeanUtil.copyProperties(item, AiTagSuggestionVO.class);
            return suggestionVO;
        }).toList());
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long acceptFollowUp(Long analysisId, AiAcceptFollowUpForm form) {
        AiAnalysis analysis = analysisMapper.selectById(analysisId);
        if (analysis == null) {
            throw new BusinessException("分析结果不存在");
        }
        AiConversation conversation = getAccessibleConversation(analysis.getConversationId());
        Long customerId = analysis.getCustomerId() != null ? analysis.getCustomerId() : conversation.getCustomerId();
        if (customerId == null) {
            throw new BusinessException("该会话未关联客户，无法生成跟进记录");
        }
        if (Integer.valueOf(1).equals(analysis.getFollowUpAccepted())) {
            return analysis.getFollowUpId();
        }
        Long userId = SecurityUtils.getUserId();
        FollowUpForm followUpForm = new FollowUpForm();
        followUpForm.setCustomerId(customerId);
        followUpForm.setType(2);
        followUpForm.setContent(StrUtil.blankToDefault(form.getContent(), analysis.getSummary()));
        followUpForm.setResult(StrUtil.blankToDefault(form.getResult(), "AI 会话摘要跟进"));
        followUpForm.setNextFollowTime(form.getNextFollowTime());
        followUpService.saveFollowUp(followUpForm);

        FollowUp followUp = followUpService.getOne(new LambdaQueryWrapper<FollowUp>()
                .eq(FollowUp::getCustomerId, customerId)
                .eq(FollowUp::getCreateBy, userId)
                .orderByDesc(FollowUp::getId)
                .last("LIMIT 1"));

        analysis.setFollowUpAccepted(1);
        analysis.setFollowUpId(followUp == null ? null : followUp.getId());
        analysis.setUpdateBy(userId);
        analysisMapper.updateById(analysis);
        return analysis.getFollowUpId();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean acceptTag(Long suggestionId) {
        AiTagSuggestion suggestion = getSuggestion(suggestionId);
        if (!Integer.valueOf(0).equals(suggestion.getStatus())) {
            throw new BusinessException("该标签建议已处理");
        }
        if (suggestion.getCustomerId() == null) {
            throw new BusinessException("该会话未关联客户，无法写入标签");
        }
        Long userId = SecurityUtils.getUserId();
        CustomerTag tag = customerTagMapper.selectOne(new LambdaQueryWrapper<CustomerTag>()
                .eq(CustomerTag::getName, suggestion.getTagName())
                .last("LIMIT 1"));
        if (tag == null) {
            tag = new CustomerTag();
            tag.setName(suggestion.getTagName());
            tag.setColor(colorOf(suggestion.getTagName()));
            tag.setSort(0);
            tag.setCreateBy(userId);
            tag.setUpdateBy(userId);
            customerTagMapper.insert(tag);
        }
        Long related = tagRelMapper.selectCount(new LambdaQueryWrapper<CustomerTagRel>()
                .eq(CustomerTagRel::getCustomerId, suggestion.getCustomerId())
                .eq(CustomerTagRel::getTagId, tag.getId()));
        if (related == null || related == 0) {
            CustomerTagRel rel = new CustomerTagRel();
            rel.setCustomerId(suggestion.getCustomerId());
            rel.setTagId(tag.getId());
            tagRelMapper.insert(rel);
        }
        suggestion.setStatus(1);
        suggestion.setTagId(tag.getId());
        tagSuggestionMapper.updateById(suggestion);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean rejectTag(Long suggestionId) {
        AiTagSuggestion suggestion = getSuggestion(suggestionId);
        if (!Integer.valueOf(0).equals(suggestion.getStatus())) {
            throw new BusinessException("该标签建议已处理");
        }
        suggestion.setStatus(2);
        tagSuggestionMapper.updateById(suggestion);
        return true;
    }

    private AiTagSuggestion getSuggestion(Long suggestionId) {
        AiTagSuggestion suggestion = tagSuggestionMapper.selectById(suggestionId);
        if (suggestion == null) {
            throw new BusinessException("标签建议不存在");
        }
        getAccessibleConversation(suggestion.getConversationId());
        return suggestion;
    }

    private Long existingTagId(String tagName) {
        CustomerTag tag = customerTagMapper.selectOne(new LambdaQueryWrapper<CustomerTag>()
                .eq(CustomerTag::getName, tagName.trim())
                .last("LIMIT 1"));
        return tag == null ? null : tag.getId();
    }

    private String tagEvidence(String tagName, List<String> candidates) {
        String best = null;
        int bestScore = 0;
        for (String candidate : candidates) {
            int score = overlapLength(tagName, candidate);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return bestScore >= 2 ? StrUtil.maxLength(best, 200) : null;
    }

    private int overlapLength(String tagName, String candidate) {
        if (StrUtil.isBlank(candidate)) {
            return 0;
        }
        int best = 0;
        for (int i = 0; i < tagName.length(); i++) {
            for (int j = i + 2; j <= tagName.length(); j++) {
                if (candidate.contains(tagName.substring(i, j))) {
                    best = Math.max(best, j - i);
                }
            }
        }
        return best;
    }

    private AiConversation getAccessibleConversation(Long id) {
        AiConversation conversation = this.getById(id);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }
        Long scope = ScrmDataScope.scopeOwnerId();
        if (scope != null
                && !Objects.equals(conversation.getOwnerId(), scope)
                && !Objects.equals(conversation.getCreateBy(), scope)) {
            throw new BusinessException("无权访问该会话");
        }
        return conversation;
    }

    private AiConversationVO toVO(AiConversation conversation, boolean includeContent) {
        AiConversationVO vo = BeanUtil.copyProperties(conversation, AiConversationVO.class);
        if (!includeContent) {
            vo.setContent(null);
        }
        return vo;
    }

    private String colorOf(String name) {
        return TAG_COLORS[(name.hashCode() & 0x7fffffff) % TAG_COLORS.length];
    }
}
