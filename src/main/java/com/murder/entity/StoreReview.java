package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门店评价实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("store_review")
public class StoreReview implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 门店ID
     */
    private Long storeId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 预约ID
     */
    private Long reservationId;
    
    /**
     * 总评分：1-5�?
     */
    private Integer rating;
    
    /**
     * 环境评分
     */
    private Integer environmentRating;
    
    /**
     * 服务评分
     */
    private Integer serviceRating;
    
    /**
     * 评价内容
     */
    private String content;
    
    /**
     * 商家回复
     */
    private String reply;
    
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
