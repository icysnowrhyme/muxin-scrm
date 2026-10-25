package com.muxin.scrm.module.ai.client;

import cn.hutool.core.util.StrUtil;
import com.muxin.scrm.module.ai.model.bo.AiChatRequest;
import com.muxin.scrm.module.ai.model.bo.AiChatResult;
import com.muxin.scrm.module.ai.model.bo.AiRuntimeConfig;
import com.muxin.scrm.module.ai.service.AiJsonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本地演示模型：不依赖外部服务，用关键词规则产出结构化结果，保证无 Key 时也能跑通闭环。
 */
@Component
@RequiredArgsConstructor
public class MockAiClient implements AiClient {

    private final AiJsonService json;

    @Override
    public AiChatResult chat(AiChatRequest request, AiRuntimeConfig config) {
        long start = System.currentTimeMillis();
        String content = switch (StrUtil.blankToDefault(request.feature(), "chat")) {
            case "summary" -> summary(request);
            case "quality" -> quality(request);
            default -> "木心 MUXIN AI 通道正常，当前使用 Mock 演示模式。";
        };
        int promptTokens = estimate(request.systemPrompt() + "\n" + request.userPrompt());
        int completionTokens = estimate(content);
        int latency = (int) (System.currentTimeMillis() - start);
        return new AiChatResult(
                content,
                "mock",
                StrUtil.blankToDefault(config.model(), "muxin-mock-v1"),
                promptTokens,
                completionTokens,
                promptTokens + completionTokens,
                Math.max(latency, 1)
        );
    }

    private String summary(AiChatRequest request) {
        String conversation = contextText(request, "conversation");
        String customer = StrUtil.blankToDefault(contextText(request, "customer"), "客户");

        List<String> demands = new ArrayList<>();
        addIfAny(demands, conversation, "价格与预算", "收费", "价格", "报价", "折扣", "预算");
        addIfAny(demands, conversation, "客户跟进与业绩统计", "跟进", "统计", "业绩");
        addIfAny(demands, conversation, "私有化部署与系统集成", "私有化", "本地", "接口", "API", "打通");
        addIfAny(demands, conversation, "会员管理", "会员", "储值", "门店");
        addIfAny(demands, conversation, "方案与成功案例", "方案", "案例", "演示");
        addIfAny(demands, conversation, "系统扩容", "扩容", "二期");
        if (demands.isEmpty()) {
            demands.add("产品功能与报价");
        }

        List<String> objections = new ArrayList<>();
        addIfAny(objections, conversation, "价格高于预期", "贵", "优惠", "低一点");
        addIfAny(objections, conversation, "预算有限或尚未确定", "预算有限", "还没定预算");
        addIfAny(objections, conversation, "正在对比其他产品", "另一家", "竞品", "对比");
        addIfAny(objections, conversation, "仍在评估阶段", "考虑", "了解一下", "初步");
        addIfAny(objections, conversation, "关注交易与数据安全", "安全吗", "数据安全", "数据要留在本地");

        List<String> commitments = new ArrayList<>();
        addIfAny(commitments, conversation, "发送方案或资料", "发一份方案", "发我一份资料", "发您", "发资料");
        addIfAny(commitments, conversation, "安排线上演示", "演示");
        addIfAny(commitments, conversation, "安排技术对接", "技术对接", "会议邀请");
        addIfAny(commitments, conversation, "协调法务复核合同", "法务");
        addIfAny(commitments, conversation, "准备新项目方案", "项目方案");
        addIfAny(commitments, conversation, "按约定时间回访", "回访", "下周再联系");

        String budget = "";
        if (containsAny(conversation, "预算有限")) {
            budget = "预算有限，价格敏感";
        } else if (containsAny(conversation, "还没定预算", "未定预算")) {
            budget = "尚未确定";
        } else if (containsAny(conversation, "预算", "报价")) {
            budget = "已进入报价沟通";
        }

        String decisionMaker = containsAny(conversation, "张总", "李总", "王总", "陈经理", "杨总", "刘工")
                ? "已直接对话决策或技术负责人"
                : "待确认决策人";

        String nextAction;
        if (containsAny(conversation, "演示")) {
            nextAction = "按约定时间准备演示并确认参会人";
        } else if (containsAny(conversation, "技术对接")) {
            nextAction = "发送技术对接会议邀请并准备接口文档";
        } else if (containsAny(conversation, "报价", "合同")) {
            nextAction = "按时发送报价与合同条款说明";
        } else if (containsAny(conversation, "回访")) {
            nextAction = "按约定时间回访，确认预算与需求";
        } else {
            nextAction = "补充确认客户需求与预算后推进下一步";
        }

        String sentiment = containsAny(conversation, "垃圾", "投诉", "不满意")
                ? "negative"
                : containsAny(conversation, "太好了", "很好", "没问题") ? "positive" : "neutral";
        String intentLevel = containsAny(conversation, "合同", "今天就定", "采购", "扩容", "报价")
                ? "high"
                : containsAny(conversation, "方案", "演示", "对接", "折扣") ? "medium" : "low";

        Set<String> tags = new LinkedHashSet<>();
        if (demands.contains("价格与预算")) {
            tags.add("价格敏感");
        }
        if (demands.contains("客户跟进与业绩统计")) {
            tags.add("关注客户运营");
        }
        if (demands.contains("私有化部署与系统集成")) {
            tags.add("技术型客户");
        }
        if (demands.contains("会员管理")) {
            tags.add("会员运营");
        }
        if (demands.contains("方案与成功案例")) {
            tags.add("重视方案案例");
        }
        if (demands.contains("系统扩容")) {
            tags.add("扩容需求");
        }
        if ("high".equals(intentLevel)) {
            tags.add("高意向");
        }
        if ("negative".equals(sentiment)) {
            tags.add("需重点跟进");
        }
        if (tags.size() < 3) {
            tags.add("私域客户");
        }
        if (tags.size() < 3) {
            tags.add("待培育");
        }

        StringBuilder summary = new StringBuilder();
        summary.append(customer).append("当前关注").append(String.join("、", demands)).append("。");
        if (!objections.isEmpty()) {
            summary.append("主要顾虑：").append(String.join("、", objections)).append("。");
        }
        if (!commitments.isEmpty()) {
            summary.append("员工已").append(String.join("、", commitments)).append("。");
        }
        summary.append("下一步：").append(nextAction).append("。");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", truncate(summary.toString(), 200));
        result.put("demands", demands);
        result.put("objections", objections);
        result.put("budget", budget);
        result.put("decisionMaker", decisionMaker);
        result.put("commitments", commitments);
        result.put("nextAction", nextAction);
        result.put("sentiment", sentiment);
        result.put("intentLevel", intentLevel);
        result.put("tags", tags.stream().limit(6).toList());
        return json.write(result);
    }

    @SuppressWarnings("unchecked")
    private String quality(AiChatRequest request) {
        String conversation = contextText(request, "conversation");
        Object rawRules = request.context() == null ? null : request.context().get("rules");
        List<Map<String, Object>> rules = rawRules instanceof List<?> list
                ? list.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList()
                : List.of();

        List<Map<String, Object>> violations = new ArrayList<>();
        int score = 100;
        for (Map<String, Object> rule : rules) {
            String code = StrUtil.blankToDefault(String.valueOf(rule.get("code")), "").toUpperCase();
            String severity = StrUtil.blankToDefault(String.valueOf(rule.get("severity")), "medium");
            String evidence = detectEvidence(code, conversation);
            if (evidence == null) {
                continue;
            }
            Map<String, Object> violation = new LinkedHashMap<>();
            violation.put("ruleCode", code);
            violation.put("hit", true);
            violation.put("severity", severity);
            violation.put("evidence", evidence);
            violation.put("reason", reasonOf(code));
            violations.add(violation);
            score -= "high".equals(severity) ? 25 : "medium".equals(severity) ? 15 : 8;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overallScore", Math.max(score, 0));
        result.put("violations", violations);
        return json.write(result);
    }

    private String detectEvidence(String code, String conversation) {
        return switch (code) {
            case "PROMISE" -> firstSentence(conversation, "保证", "绝对", "一定能", "百分百", "最低价");
            case "PRIVATE_DEAL" -> firstSentence(conversation, "私人微信", "私下", "转账");
            case "RUDE" -> firstSentence(conversation, "闭嘴", "滚", "废物", "蠢");
            case "PRIVACY" -> firstSentence(conversation, "身份证", "银行卡", "密码");
            case "COMPETITOR" -> firstSentence(conversation, "垃圾", "骗子", "不靠谱", "别用");
            case "SOP" -> sopEvidence(conversation);
            default -> null;
        };
    }

    private String sopEvidence(String conversation) {
        List<String> missing = new ArrayList<>();
        if (!conversation.contains("预算")) {
            missing.add("预算");
        }
        if (!containsAny(conversation, "决策", "负责人")) {
            missing.add("决策人");
        }
        if (!containsAny(conversation, "下周", "明天", "下午", "上午", "约", "时间")) {
            missing.add("下一步时间");
        }
        if (!containsAny(conversation, "需求", "想解决", "想要", "了解")) {
            missing.add("客户需求");
        }
        if (missing.size() < 2) {
            return null;
        }
        return "会话未完整确认：" + String.join("、", missing);
    }

    private String reasonOf(String code) {
        return switch (code) {
            case "PROMISE" -> "出现收益承诺或绝对化表述，违反广告合规要求";
            case "PRIVATE_DEAL" -> "引导私下交易，存在飞单与资金风险";
            case "RUDE" -> "服务用语不当，可能引发客户投诉";
            case "PRIVACY" -> "涉及敏感个人信息，存在合规风险";
            case "COMPETITOR" -> "使用贬损性表述评价竞品";
            case "SOP" -> "关键销售信息未确认，流程不完整";
            default -> "命中质检规则";
        };
    }

    private String firstSentence(String conversation, String... keywords) {
        for (String line : conversation.split("[。！？\\n]")) {
            String sentence = line.trim();
            if (sentence.isEmpty()) {
                continue;
            }
            for (String keyword : keywords) {
                if (sentence.contains(keyword)) {
                    return truncate(sentence, 200);
                }
            }
        }
        return null;
    }

    private void addIfAny(List<String> target, String text, String value, String... keywords) {
        if (containsAny(text, keywords)) {
            target.add(value);
        }
    }

    private boolean containsAny(String text, String... keywords) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String contextText(AiChatRequest request, String key) {
        Object value = request.context() == null ? null : request.context().get(key);
        return value == null ? "" : value.toString();
    }

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }

    private int estimate(String text) {
        return Math.max(1, StrUtil.length(text) / 2);
    }
}
