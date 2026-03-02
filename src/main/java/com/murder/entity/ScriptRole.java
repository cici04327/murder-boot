package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 剧本角色实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("script_role")
public class ScriptRole implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 剧本ID
     */
    private Long scriptId;
    
    /**
     * 角色名称
     */
    @TableField("name")
    private String roleName;
    
    /**
     * 角色头像图片URL
     */
    private String avatar;
    
    /**
     * 角色立绘图片URL
     */
    private String characterImage;
    
    /**
     * 性别要求�?男，2女，3不限
     */
    private Integer gender;
    
    /**
     * 年龄要求
     */
    private String ageRange;
    
    /**
     * 角色描述
     */
    private String description;
    
    /**
     * 角色难度�?简单，2普通，3困难
     */
    private Integer difficulty;
    
    /**
     * 排序
     */
    private Integer sort;
    
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