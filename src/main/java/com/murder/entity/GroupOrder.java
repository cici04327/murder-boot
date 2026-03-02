package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼单实体类
 */
@Data
@TableName("group_order")
public class GroupOrder {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 发起人ID */
    private Long creatorId;
    
    /** 发起人名称 */
    private String creatorName;
    
    /** 发起人头像 */
    private String creatorAvatar;
    
    /** 剧本ID */
    private Long scriptId;
    
    /** 剧本名称 */
    private String scriptName;
    
    /** 门店ID */
    private Long storeId;
    
    /** 门店名称 */
    private String storeName;
    
    /** 开车时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime playTime;
    
    /** 当前人数 */
    private Integer currentCount;
    
    /** 需要总人数 */
    private Integer needCount;
    
    /** 剧本人数 */
    private Integer playerCount;
    
    /** 价格（每人） */
    private BigDecimal price;
    
    /** 性别要求 */
    private String genderRequirement;
    
    /** 新手友好 0-否 1-是 */
    private Integer newbieWelcome;
    
    /** 拼单说明 */
    private String description;
    
    /** 关联预约ID */
    private Long reservationId;
    
    /** 状态 0-已取消 1-拼团中 2-已成团 3-已结束 */
    private Integer status;
    
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
