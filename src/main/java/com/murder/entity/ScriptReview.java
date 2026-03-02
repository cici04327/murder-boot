package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 剧本评价实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("script_review")
public class ScriptReview implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 剧本ID
     */
    private Long scriptId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 预约ID
     */
    private Long reservationId;
    
    /**
     * 评分�?-5�?
     */
    private Integer rating;
    
    /**
     * 剧情评分
     */
    private Integer storyRating;
    
    /**
     * 难度评分
     */
    private Integer difficultyRating;
    
    /**
     * 评价内容
     */
    private String content;
    
    /**
     * 逻辑删除�?删除�?未删�?
     */
    @TableLogic
    private Integer isDeleted;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
