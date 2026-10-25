package com.muxin.scrm.module.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI 模型连通性测试结果")
public class AiTestResultVO {

    private String provider;
    private String model;
    private Boolean mocked;
    private Integer latencyMs;
    private String message;
}
