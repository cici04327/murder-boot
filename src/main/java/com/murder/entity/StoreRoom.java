package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门店房间实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("store_room")
public class StoreRoom implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 门店ID
     */
    private Long storeId;
    
    /**
     * 房间名称
     */
    private String name;
    
    /**
     * 房间类型�?小房�?中房�?大房
     */
    private Integer type;
    
    /**
     * 容纳人数
     */
    private Integer capacity;
    
    /**
     * 房间图片
     */
    @TableField(exist = false)
    private String images;
    
    /**
     * 房间描述
     */
    private String description;
    
    /**
     * 状态：1可用�?不可�?
     */
    private Integer status;
    
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
