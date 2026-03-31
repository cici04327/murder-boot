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

/**
 * AI排班任务记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_schedule_task")
public class AiScheduleTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 门店ID
     */
    private Long storeId;

    /**
     * 任务类型：DM_ASSIGN-DM分配，EMPLOYEE_SCHEDULE-员工排班
     */
    private String taskType;

    /**
     * 排班开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate startDate;

    /**
     * 排班结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate endDate;

    /**
     * 状态：PENDING/RUNNING/DONE/FAILED
     */
    private String status;

    /**
     * 需处理总数
     */
    private Integer totalCount;

    /**
     * 成功处理数
     */
    private Integer successCount;

    /**
     * AI生成的排班说明/总结
     */
    private String resultSummary;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 触发方式：MANUAL-手动，AUTO-自动
     */
    private String triggerType;

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
    private String storeName;
}
