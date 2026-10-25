package com.muxin.scrm.module.ai.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muxin.scrm.core.exception.BusinessException;
import com.muxin.scrm.module.ai.mapper.AiQualityRuleMapper;
import com.muxin.scrm.module.ai.model.entity.AiQualityRule;
import com.muxin.scrm.module.ai.model.form.AiQualityRuleForm;
import com.muxin.scrm.security.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiQualityRuleService extends ServiceImpl<AiQualityRuleMapper, AiQualityRule> {

    public List<AiQualityRule> listRules(String keyword, Integer enabled) {
        LambdaQueryWrapper<AiQualityRule> wrapper = new LambdaQueryWrapper<AiQualityRule>()
                .eq(enabled != null, AiQualityRule::getEnabled, enabled)
                .orderByAsc(AiQualityRule::getSort)
                .orderByAsc(AiQualityRule::getId);
        if (StrUtil.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(AiQualityRule::getCode, kw).or().like(AiQualityRule::getName, kw));
        }
        return this.list(wrapper);
    }

    public List<AiQualityRule> listEnabledRules(List<Long> ids) {
        LambdaQueryWrapper<AiQualityRule> wrapper = new LambdaQueryWrapper<AiQualityRule>()
                .eq(AiQualityRule::getEnabled, 1)
                .in(CollUtil.isNotEmpty(ids), AiQualityRule::getId, ids)
                .orderByAsc(AiQualityRule::getSort)
                .orderByAsc(AiQualityRule::getId);
        return this.list(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean saveRule(AiQualityRuleForm form) {
        String code = form.getCode().trim().toUpperCase();
        checkCodeUnique(code, null);
        AiQualityRule rule = new AiQualityRule();
        rule.setCode(code);
        fill(rule, form);
        rule.setCreateBy(SecurityUtils.getUserId());
        rule.setUpdateBy(SecurityUtils.getUserId());
        return this.save(rule);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateRule(Long id, AiQualityRuleForm form) {
        AiQualityRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException("质检规则不存在");
        }
        String code = form.getCode().trim().toUpperCase();
        checkCodeUnique(code, id);
        rule.setCode(code);
        fill(rule, form);
        rule.setUpdateBy(SecurityUtils.getUserId());
        rule.setUpdateTime(LocalDateTime.now());
        return this.updateById(rule);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRules(String ids) {
        List<Long> idList = StrUtil.split(ids, ',').stream().filter(StrUtil::isNotBlank)
                .map(item -> Long.valueOf(item.trim())).toList();
        if (CollUtil.isEmpty(idList)) {
            return false;
        }
        return this.removeByIds(idList);
    }

    private void fill(AiQualityRule rule, AiQualityRuleForm form) {
        rule.setName(form.getName().trim());
        rule.setCategory(StrUtil.trimToNull(form.getCategory()));
        rule.setDescription(StrUtil.trimToNull(form.getDescription()));
        rule.setInstruction(form.getInstruction().trim());
        rule.setSeverity(StrUtil.blankToDefault(form.getSeverity(), "medium"));
        rule.setEnabled(form.getEnabled() == null ? 1 : form.getEnabled());
        rule.setSort(form.getSort() == null ? 0 : form.getSort());
    }

    private void checkCodeUnique(String code, Long excludeId) {
        boolean exists = this.exists(new LambdaQueryWrapper<AiQualityRule>()
                .eq(AiQualityRule::getCode, code)
                .ne(excludeId != null, AiQualityRule::getId, excludeId));
        if (exists) {
            throw new BusinessException("规则编码已存在");
        }
    }
}
