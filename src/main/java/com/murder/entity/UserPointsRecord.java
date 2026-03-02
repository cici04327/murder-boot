package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户积分记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_points_record")
public class UserPointsRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 积分变化：正数为增加，负数为减少
     */
    private Integer points;
    
    /**
     * 类型�?增加�?减少
     */
    private Integer type;
    
    /**
     * 积分来源�?注册赠送，2消费获得�?签到奖励�?活动奖励�?管理员调�?
     */
    private Integer source;
    
    /**
     * 描述
     */
    private String description;
    
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
}
