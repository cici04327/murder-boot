package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 门店员工实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("store_employee")
public class StoreEmployee implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 门店ID
     */
    private Long storeId;
    
    /**
     * 员工姓名
     */
    private String name;
    
    /**
     * 联系电话
     */
    private String phone;
    
    /**
     * 职位�?店长�?副店长，3主持人，4服务�?
     */
    private Integer position;
    
    /**
     * 入职日期
     */
    private LocalDate joinDate;
    
    /**
     * 月薪
     */
    private BigDecimal salary;
    
    /**
     * 状态：1在职�?离职
     */
    private Integer status;

    /**
     * 登录账号
     */
    private String loginAccount;

    /**
     * 登录密码（MD5）
     */
    private String loginPassword;

    /**
     * 员工后台角色：MANAGER / CLERK / DM
     */
    private String staffRole;

    /**
     * 权限编码，逗号分隔
     */
    private String permissionCodes;

    /**
     * 绑定DM ID（主持人账号可选）
     */
    private Long dmId;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    
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
