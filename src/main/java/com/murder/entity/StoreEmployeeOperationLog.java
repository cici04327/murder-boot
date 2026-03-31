package com.murder.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门店员工操作日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("store_employee_operation_log")
public class StoreEmployeeOperationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storeId;

    private Long operatorId;

    private Long employeeId;

    private String operatorType;

    private String operatorRole;

    private String operatorName;

    private String actionType;

    private String targetType;

    private Long targetId;

    private String targetName;

    private String detail;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
