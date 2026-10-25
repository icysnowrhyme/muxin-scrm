package com.muxin.scrm.module.ai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.muxin.scrm.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("ai_conversation")
public class AiConversation extends BaseEntity {

    private String title;
    private Long customerId;
    private String customerName;
    private String employeeName;
    private Long ownerId;
    private String source;
    private String content;
    private Integer messageCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    private Integer status;
    private Long createBy;
    private Long updateBy;
    private Integer isDeleted;
}
