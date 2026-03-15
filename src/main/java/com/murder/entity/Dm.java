package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DM（主持人）实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dm")
public class Dm implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属门店ID
     */
    private Long storeId;

    /**
     * DM 姓名/艺名
     */
    private String name;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 风格标签，逗号分隔（如：推理型,情感型,恐怖型）
     */
    private String styleTags;

    /**
     * 综合评分（由评价自动计算，精度0.1）
     */
    private BigDecimal rating;

    /**
     * 累计主持场次
     */
    private Integer gameCount;

    /**
     * 状态：1在职，0离职
     */
    private Integer status;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
