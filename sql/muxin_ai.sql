-- 木心 MUXIN · AI 模块增量脚本
-- 适用于已有 muxin_scrm 数据库；可重复执行，不会覆盖已有业务数据。
-- 全新部署时由 docker-compose 在 01-muxin_scrm.sql 之后自动执行。

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `muxin_scrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `muxin_scrm`;

SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 模型配置
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_model_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '配置名称',
  `provider` varchar(32) NOT NULL DEFAULT 'mock' COMMENT 'mock / openai',
  `base_url` varchar(255) DEFAULT NULL COMMENT 'OpenAI 兼容接口地址',
  `api_key_cipher` varchar(1024) DEFAULT NULL COMMENT 'API Key 密文',
  `model` varchar(128) NOT NULL COMMENT '模型名称',
  `temperature` decimal(3,2) DEFAULT '0.30' COMMENT '温度',
  `max_tokens` int DEFAULT '1200' COMMENT '最大输出 token',
  `enabled` tinyint DEFAULT '1' COMMENT '是否启用',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认配置',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 模型配置';

-- ---------------------------------------------------------------------------
-- 会话
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(128) NOT NULL COMMENT '会话标题',
  `customer_id` bigint DEFAULT NULL COMMENT '关联客户',
  `customer_name` varchar(100) DEFAULT NULL COMMENT '客户名称快照',
  `employee_name` varchar(64) DEFAULT NULL COMMENT '员工名称快照',
  `owner_id` bigint DEFAULT NULL COMMENT '归属员工用户 ID',
  `source` varchar(32) DEFAULT 'import' COMMENT '来源：wecom/import/mock',
  `content` longtext NOT NULL COMMENT '会话原文',
  `message_count` int DEFAULT '0' COMMENT '消息条数',
  `started_at` datetime DEFAULT NULL COMMENT '会话开始时间',
  `status` tinyint DEFAULT '0' COMMENT '0 未分析，1 已分析',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_conversation_customer` (`customer_id`),
  KEY `idx_ai_conversation_owner` (`owner_id`),
  KEY `idx_ai_conversation_time` (`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 会话数据';

-- ---------------------------------------------------------------------------
-- 会话分析（摘要 / 跟进草稿 / 结构化画像）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_analysis` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL,
  `customer_id` bigint DEFAULT NULL,
  `summary` varchar(1000) DEFAULT NULL COMMENT '会话摘要',
  `demands_json` text COMMENT '客户需求 JSON 数组',
  `objections_json` text COMMENT '客户异议 JSON 数组',
  `budget` varchar(255) DEFAULT NULL COMMENT '预算',
  `decision_maker` varchar(255) DEFAULT NULL COMMENT '决策人',
  `commitments_json` text COMMENT '承诺事项 JSON 数组',
  `next_action` varchar(500) DEFAULT NULL COMMENT '下一步建议',
  `sentiment` varchar(16) DEFAULT NULL COMMENT 'positive/neutral/negative',
  `intent_level` varchar(16) DEFAULT NULL COMMENT 'high/medium/low',
  `tags_json` text COMMENT '标签建议 JSON 数组',
  `raw_output` longtext COMMENT '模型原始输出',
  `provider` varchar(32) DEFAULT NULL,
  `model` varchar(128) DEFAULT NULL,
  `prompt_tokens` int DEFAULT '0',
  `completion_tokens` int DEFAULT '0',
  `total_tokens` int DEFAULT '0',
  `latency_ms` int DEFAULT '0',
  `follow_up_accepted` tinyint DEFAULT '0' COMMENT '是否已采纳为跟进',
  `follow_up_id` bigint DEFAULT NULL COMMENT '生成的跟进记录 ID',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_analysis_conversation` (`conversation_id`),
  KEY `idx_ai_analysis_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 会话分析';

-- ---------------------------------------------------------------------------
-- 标签建议
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_tag_suggestion` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL,
  `analysis_id` bigint DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `tag_id` bigint DEFAULT NULL COMMENT '命中已有标签时写入',
  `tag_name` varchar(64) NOT NULL COMMENT '标签名称',
  `confidence` decimal(5,2) DEFAULT '80.00' COMMENT '置信度',
  `evidence` varchar(500) DEFAULT NULL COMMENT '依据',
  `status` tinyint DEFAULT '0' COMMENT '0 待处理，1 已采纳，2 已忽略',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_tag_conversation` (`conversation_id`),
  KEY `idx_ai_tag_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 标签建议';

-- ---------------------------------------------------------------------------
-- 质检规则
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_quality_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL COMMENT '规则编码',
  `name` varchar(128) NOT NULL,
  `category` varchar(64) DEFAULT NULL COMMENT '分类',
  `description` varchar(500) DEFAULT NULL,
  `instruction` text NOT NULL COMMENT '判定说明',
  `severity` varchar(16) DEFAULT 'medium' COMMENT 'high/medium/low',
  `enabled` tinyint DEFAULT '1',
  `sort` int DEFAULT '0',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_quality_rule_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 质检规则';

-- ---------------------------------------------------------------------------
-- 质检任务
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_quality_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `conversation_ids` text NOT NULL COMMENT '会话 ID，逗号分隔',
  `rule_ids` text COMMENT '规则 ID，逗号分隔；为空表示全部启用规则',
  `total_count` int DEFAULT '0',
  `processed_count` int DEFAULT '0',
  `hit_count` int DEFAULT '0',
  `status` tinyint DEFAULT '0' COMMENT '0 待执行，1 执行中，2 成功，3 失败',
  `error_message` varchar(1000) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_quality_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 质检任务';

-- ---------------------------------------------------------------------------
-- 质检结果
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_quality_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `conversation_id` bigint NOT NULL,
  `customer_name` varchar(100) DEFAULT NULL,
  `rule_id` bigint DEFAULT NULL,
  `rule_code` varchar(64) DEFAULT NULL,
  `rule_name` varchar(128) DEFAULT NULL,
  `severity` varchar(16) DEFAULT NULL,
  `hit` tinyint DEFAULT '0' COMMENT '是否命中',
  `evidence` varchar(1000) DEFAULT NULL COMMENT '违规原文',
  `reason` varchar(1000) DEFAULT NULL COMMENT '判定理由',
  `score` int DEFAULT '100' COMMENT '单项得分',
  `review_status` tinyint DEFAULT '0' COMMENT '0 待复核，1 确认违规，2 误报',
  `review_note` varchar(500) DEFAULT NULL,
  `reviewer_id` bigint DEFAULT NULL,
  `review_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_quality_result_task` (`task_id`),
  KEY `idx_ai_quality_result_conversation` (`conversation_id`),
  KEY `idx_ai_quality_result_hit` (`hit`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 质检结果';

-- ---------------------------------------------------------------------------
-- 用量日志
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_usage_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `feature` varchar(32) NOT NULL COMMENT 'summary/quality/test',
  `provider` varchar(32) DEFAULT NULL,
  `model` varchar(128) DEFAULT NULL,
  `prompt_tokens` int DEFAULT '0',
  `completion_tokens` int DEFAULT '0',
  `total_tokens` int DEFAULT '0',
  `latency_ms` int DEFAULT '0',
  `success` tinyint DEFAULT '1',
  `error_message` varchar(1000) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_usage_feature` (`feature`),
  KEY `idx_ai_usage_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 调用用量';

-- ---------------------------------------------------------------------------
-- Prompt 模板
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_prompt_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL COMMENT 'summary/quality',
  `name` varchar(128) NOT NULL,
  `system_prompt` text NOT NULL,
  `user_template` text NOT NULL,
  `version` int DEFAULT '1',
  `enabled` tinyint DEFAULT '1',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_prompt_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI Prompt 模板';

-- ---------------------------------------------------------------------------
-- 初始化数据
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `ai_model_config`
(`id`, `name`, `provider`, `base_url`, `api_key_cipher`, `model`, `temperature`, `max_tokens`, `enabled`, `is_default`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
(1, '默认 Mock 模型', 'mock', NULL, NULL, 'muxin-mock-v1', 0.30, 1200, 1, 1, 1, NOW(), 1, NOW());

INSERT IGNORE INTO `ai_prompt_template`
(`id`, `code`, `name`, `system_prompt`, `user_template`, `version`, `enabled`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
(1, 'summary', '会话摘要与客户画像',
'你是企业私域销售助理，擅长把销售与客户的会话整理成结构化客户情报。\n请严格输出 JSON，不要输出 Markdown 代码块，不要编造会话中没有的信息。\n字段：\nsummary: 80字以内的会话摘要\ndemands: 客户需求数组\nobjections: 客户异议数组\nbudget: 预算信息，没有则空字符串\ndecisionMaker: 决策人信息，没有则空字符串\ncommitments: 双方承诺数组\nnextAction: 下一步建议，一句话\nsentiment: positive/neutral/negative\nintentLevel: high/medium/low\ntags: 3到6个简短客户标签数组',
'客户：{customer}\n会话记录：\n{conversation}', 1, 1, 1, NOW(), 1, NOW()),
(2, 'quality', '会话质检',
'你是企业私域会话质检员。根据给定规则检查会话，严格输出 JSON，不要输出 Markdown。\n输出格式：{"overallScore":0到100的整数,"violations":[{"ruleCode":"规则编码","hit":true,"severity":"high|medium|low","evidence":"违规原文片段","reason":"判定理由"}]}\n只输出命中的规则；如果没有违规，violations 为空数组。',
'质检规则：\n{rules}\n\n会话记录：\n{conversation}', 1, 1, 1, NOW(), 1, NOW());

INSERT IGNORE INTO `ai_quality_rule`
(`id`, `code`, `name`, `category`, `description`, `instruction`, `severity`, `enabled`, `sort`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
(1, 'PROMISE', '违规承诺', '合规风险', '承诺收益、效果或使用绝对化表述', '禁止承诺收益、效果、结果或使用绝对化表述，如保证、百分百、一定能、绝对有效。', 'high', 1, 1, 1, NOW(), 1, NOW()),
(2, 'PRIVATE_DEAL', '私下交易', '合规风险', '引导私下转账或添加私人联系方式', '禁止引导客户添加私人微信、私下转账、绕开公司收款或索要私人联系方式。', 'high', 1, 2, 1, NOW(), 1, NOW()),
(3, 'RUDE', '服务态度', '服务规范', '辱骂、嘲讽或威胁客户', '禁止辱骂、嘲讽、威胁客户或使用不礼貌用语。', 'high', 1, 3, 1, NOW(), 1, NOW()),
(4, 'PRIVACY', '隐私泄露', '合规风险', '索要或泄露敏感个人信息', '禁止索要或泄露身份证号、银行卡号、密码等敏感个人信息。', 'high', 1, 4, 1, NOW(), 1, NOW()),
(5, 'COMPETITOR', '竞品诋毁', '服务规范', '使用侮辱性语言评价竞品', '禁止使用侮辱性语言诋毁竞品，应客观比较。', 'medium', 1, 5, 1, NOW(), 1, NOW()),
(6, 'SOP', '流程规范', '流程规范', '未确认需求、预算、决策人或下一步时间', '应主动确认客户需求、预算、决策人和下一步时间，未确认视为流程不规范。', 'low', 1, 6, 1, NOW(), 1, NOW());

INSERT IGNORE INTO `ai_conversation`
(`id`, `title`, `customer_id`, `customer_name`, `employee_name`, `owner_id`, `source`, `content`, `message_count`, `started_at`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES
(1, '海川科技年度采购沟通', 1, '张伟', '王敏', 2, 'wecom',
'客户：你们这套系统怎么收费？我们今年预算有限。\n员工：张总，标准版一年 2.8 万，包含 20 个账号。\n客户：比我们现在的贵一些，能不能再优惠？\n员工：我保证给您申请到最低价，而且效果绝对没问题。\n客户：主要想解决客户跟进和业绩统计。\n员工：没问题，这两个模块我们都有，我下午发一份方案给您。\n客户：可以，最好带上同行业的案例。\n员工：好的，另外我们下周可以安排一次线上演示。\n客户：下周三下午我有时间。\n员工：那我先约下周三下午三点。',
10, '2026-10-21 10:00:00', 0, 2, NOW(), 2, NOW(), 0),
(2, '星辰传媒私域工具选型', 2, '李娜', '赵磊', 4, 'wecom',
'客户：我们主要做短视频内容，想看看你们的私域工具。\n员工：李总，我们支持企微客户沉淀、群发和内容素材库。\n客户：我们也在看另一家，叫 XX SCRM，价格比你们低。\n员工：那家的产品很垃圾，别用了。\n客户：……我先了解一下。\n员工：建议您重点看客户画像和转化报表。\n客户：可以，发我一份资料。\n员工：好的，您更方便微信还是邮箱？\n客户：邮箱吧。',
9, '2026-10-20 15:30:00', 0, 4, NOW(), 4, NOW(), 0),
(3, '恒信建材折扣申请', 3, '王强', '孙悦', 5, 'wecom',
'客户：上次说的折扣能再低一点吗？\n员工：王总，公司政策最多九折。\n客户：如果能再低，我今天就定。\n员工：这样吧，您加我私人微信，我私下给您转账优惠。\n客户：这样安全吗？\n员工：放心，很多人都这么操作。\n客户：那我考虑一下。\n员工：好的，尽快给我答复。',
8, '2026-10-19 14:00:00', 0, 5, NOW(), 5, NOW(), 0),
(4, '极客网络接口集成评估', 4, '刘洋', '王敏', 2, 'wecom',
'客户：你们的开放接口支持私有化部署吗？\n员工：刘工，支持，我们可以提供 API 文档和沙箱环境。\n客户：我们需要和内部 CRM 打通，数据要留在本地。\n员工：可以，私有化部署数据完全在您这边。\n客户：那太好了，什么时候能安排技术对接？\n员工：本周五下午可以吗？\n客户：可以，我把我们技术同事拉上。\n员工：好的，我提前发会议邀请。',
8, '2026-10-22 09:20:00', 0, 2, NOW(), 2, NOW(), 0),
(5, '悦享餐饮会员需求初探', 5, '陈静', '孙悦', 5, 'wecom',
'客户：我们门店想做一个会员管理，先了解一下。\n员工：陈经理，我们有会员标签、储值和新客活动。\n客户：目前只是初步看看，还没定预算。\n员工：没关系，可以先加个微信，我把资料发您。\n客户：好的，有需要再联系。\n员工：那我下周再回访您。',
6, '2026-10-18 16:40:00', 0, 5, NOW(), 5, NOW(), 0),
(6, '云启软件二期扩容', 6, '杨磊', '李静', 1, 'wecom',
'客户：二期扩容的报价什么时候能出来？\n员工：杨总，今天下班前发您。\n客户：合同条款里数据安全那块要再确认一下。\n员工：没问题，我让法务同事一起看。\n客户：好，另外下个月我们有个新项目，也可以聊聊。\n员工：太好了，我准备一份项目方案。\n客户：可以，下周约个时间。',
7, '2026-10-22 11:10:00', 0, 1, NOW(), 1, NOW(), 0);

-- ---------------------------------------------------------------------------
-- 权限点（按钮级，供 @PreAuthorize 校验）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `sys_menu`
(`id`, `parent_id`, `tree_path`, `name`, `type`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`)
VALUES
(9501, 0, '0', 'AI 设置查询', 'B', 'ai:config:list', 0, 0, 0, 1),
(9502, 0, '0', 'AI 设置保存', 'B', 'ai:config:update', 0, 0, 0, 2),
(9503, 0, '0', 'AI 连通性测试', 'B', 'ai:config:test', 0, 0, 0, 3),
(9504, 0, '0', 'AI 质检规则维护', 'B', 'ai:quality:rule', 0, 0, 0, 4);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES
(2, 9501), (2, 9502), (2, 9503), (2, 9504),
(4, 9504);

SET FOREIGN_KEY_CHECKS = 1;
