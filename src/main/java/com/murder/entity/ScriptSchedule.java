package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 剧本排期实体
 * 用于设置剧本在特定日期的可预约时段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("script_schedule")
public class ScriptSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 门店ID
     */
    private Long storeId;

    /**
     * 剧本ID
     */
    private Long scriptId;

    /**
     * 房间ID
     */
    private Long roomId;

    /**
     * 排期日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate scheduleDate;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    private LocalTime endTime;

    /**
     * 最大预约人数
     */
    private Integer maxPlayers;

    /**
     * 当前已预约人数
     */
    private Integer currentPlayers;

    /**
     * 状态：1可预约，0已满，2已关闭
     */
    private Integer status;

    /**
     * 主持 DM 的 ID（可为空，表示暂未分配）
     */
    private Long dmId;

    /**
     * 备注
     */
    private String remark;

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

    // 非数据库字段
    @TableField(exist = false)
    private String scriptName;

    @TableField(exist = false)
    private String roomName;

    /** DM 姓名（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String dmName;

    /** DM 头像（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String dmAvatar;
}
