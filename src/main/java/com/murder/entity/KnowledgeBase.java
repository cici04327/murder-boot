package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI知识库实体
 * 存储平台业务知识，用于RAG检索增强生成
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_knowledge_base")
public class KnowledgeBase implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识分类：reservation/refund/vip/coupon/points/group/payment/dm/store/system
     */
    private String category;

    /**
     * 知识标题（也作为检索关键词索引）
     */
    private String title;

    /**
     * 知识内容（实际注入Prompt的文本）
     */
    private String content;

    /**
     * 关键词（逗号分隔，用于检索匹配）
     */
    private String keywords;

    /**
     * 优先级（越大越优先，用于同分时排序）
     */
    private Integer priority;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
