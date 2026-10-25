package com.muxin.scrm.module.ai.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "AI 会话分析结果")
public class AiAnalysisVO {

    private Long id;
    private Long conversationId;
    private Long customerId;
    private String summary;
    private List<String> demands;
    private List<String> objections;
    private String budget;
    private String decisionMaker;
    private List<String> commitments;
    private String nextAction;
    private String sentiment;
    private String intentLevel;
    private List<String> tags;
    private String provider;
    private String model;
    private Integer totalTokens;
    private Integer latencyMs;
    private Integer followUpAccepted;
    private Long followUpId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private List<AiTagSuggestionVO> suggestions;
}
