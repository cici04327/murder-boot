package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评价实体（综合评价表�?
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("review")
public class Review implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 预约ID
     */
    private Long reservationId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 门店ID
     */
    private Long storeId;
    
    /**
     * 剧本ID
     */
    private Long scriptId;
    
    /**
     * 综合评分�?-5�?
     */
    private Integer overallRating;
    
    /**
     * 门店评分
     */
    private Integer storeRating;
    
    /**
     * 剧本评分
     */
    private Integer scriptRating;
    
    /**
     * 服务评分
     */
    private Integer serviceRating;

    /**
     * DM ID（主持人）
     */
    private Long dmId;

    /**
     * DM 评分（1-5星）
     */
    private Integer dmRating;
    
    /**
     * 评价内容
     */
    private String content;
    
    /**
     * 评价图片，逗号分隔
     */
    private String images;
    
    /**
     * 评价标签，逗号分隔
     */
    private String tags;
    
    /**
     * 状态：1待审核，2已通过�?已拒�?
     */
    private Integer status;
    
    /**
     * 是否匿名�?是，0�?
     */
    private Integer isAnonymous;
    
    /**
     * 是否精选：1是，0�?
     */
    private Integer isFeatured;
    
    /**
     * 商家回复内容
     */
    private String replyContent;
    
    /**
     * 回复时间
     */
    private LocalDateTime replyTime;
    
    /**
     * 奖励积分
     */
    private Integer rewardPoints;
    
    /**
     * 奖励优惠券ID
     */
    private Long rewardCouponId;
    
    /**
     * 审核人ID
     */
    private Long auditUserId;
    
    /**
     * 审核时间
     */
    private LocalDateTime auditTime;
    
    /**
     * 审核原因
     */
    private String auditReason;
    
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
