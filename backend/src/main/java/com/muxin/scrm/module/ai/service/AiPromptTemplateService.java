package com.muxin.scrm.module.ai.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muxin.scrm.core.exception.BusinessException;
import com.muxin.scrm.module.ai.mapper.AiPromptTemplateMapper;
import com.muxin.scrm.module.ai.model.bo.AiPromptPair;
import com.muxin.scrm.module.ai.model.entity.AiPromptTemplate;
import com.muxin.scrm.module.ai.model.form.AiPromptTemplateForm;
import com.muxin.scrm.security.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AiPromptTemplateService extends ServiceImpl<AiPromptTemplateMapper, AiPromptTemplate> {

    private static final String DEFAULT_SUMMARY_SYSTEM = """
            你是企业私域销售助理，擅长把销售与客户的会话整理成结构化客户情报。
            请严格输出 JSON，不要输出 Markdown 代码块，不要编造会话中没有的信息。
            字段：
            summary: 80字以内的会话摘要
            demands: 客户需求数组
            objections: 客户异议数组
            budget: 预算信息，没有则空字符串
            decisionMaker: 决策人信息，没有则空字符串
            commitments: 双方承诺数组
            nextAction: 下一步建议，一句话
            sentiment: positive/neutral/negative
            intentLevel: high/medium/low
            tags: 3到6个简短客户标签数组""";

    private static final String DEFAULT_SUMMARY_USER = """
            客户：{customer}
            会话记录：
            {conversation}""";

    private static final String DEFAULT_QUALITY_SYSTEM = """
            你是企业私域会话质检员。根据给定规则检查会话，严格输出 JSON，不要输出 Markdown。
            输出格式：{"overallScore":0到100的整数,"violations":[{"ruleCode":"规则编码","hit":true,"severity":"high|medium|low","evidence":"违规原文片段","reason":"判定理由"}]}
            只输出命中的规则；如果没有违规，violations 为空数组。""";

    private static final String DEFAULT_QUALITY_USER = """
            质检规则：
            {rules}

            会话记录：
            {conversation}""";

    public AiPromptPair getPair(String code) {
        AiPromptTemplate template = this.getOne(new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getCode, code)
                .eq(AiPromptTemplate::getEnabled, 1)
                .last("LIMIT 1"));
        if (template != null) {
            return new AiPromptPair(template.getCode(), template.getSystemPrompt(),
                    template.getUserTemplate(), template.getVersion());
        }
        return switch (code) {
            case "summary" -> new AiPromptPair(code, DEFAULT_SUMMARY_SYSTEM, DEFAULT_SUMMARY_USER, 0);
            case "quality" -> new AiPromptPair(code, DEFAULT_QUALITY_SYSTEM, DEFAULT_QUALITY_USER, 0);
            default -> throw new BusinessException("Prompt 模板不存在：" + code);
        };
    }

    public String render(String template, Map<String, String> variables) {
        String result = StrUtil.nullToEmpty(template);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", StrUtil.nullToEmpty(entry.getValue()));
        }
        return result;
    }

    public List<AiPromptTemplate> listTemplates() {
        return this.list(new LambdaQueryWrapper<AiPromptTemplate>().orderByAsc(AiPromptTemplate::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public AiPromptTemplate updateTemplate(String code, AiPromptTemplateForm form) {
        AiPromptTemplate template = this.getOne(new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getCode, code)
                .last("LIMIT 1"));
        boolean created = template == null;
        if (created) {
            template = new AiPromptTemplate();
            template.setCode(code);
            template.setVersion(1);
            template.setCreateBy(SecurityUtils.getUserId());
        } else {
            template.setVersion(template.getVersion() == null ? 2 : template.getVersion() + 1);
        }
        template.setName(form.getName().trim());
        template.setSystemPrompt(form.getSystemPrompt());
        template.setUserTemplate(form.getUserTemplate());
        template.setEnabled(form.getEnabled() == null ? 1 : form.getEnabled());
        template.setUpdateBy(SecurityUtils.getUserId());
        if (created) {
            this.save(template);
        } else {
            template.setUpdateTime(LocalDateTime.now());
            this.updateById(template);
        }
        return template;
    }
}
