package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户�?     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 手机�?     */
    private String phone;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 性别�?男，2�?     */
    private Integer gender;
    
    /**
     * 会员等级�?普通，2银卡�?金卡�?钻石
     */
    private Integer memberLevel;
    
    /**
     * VIP等级�?非会员，1普通，2银卡�?金卡�?钻石
     */
    private Integer vipLevel;
    
    /**
     * VIP到期时间
     */
    private LocalDateTime vipExpireTime;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 生日
     */
    private java.time.LocalDate birthday;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 实名认证状态：0未认证，1已认证
     */
    private Integer realNameVerified;

    /**
     * 手机是否已验证：0未验证，1已验证
     */
    private Integer phoneVerified;

    /**
     * 邮箱是否已验证：0未验证，1已验证
     */
    private Integer emailVerified;
    
    /**
     * 所在城�?     */
    @TableField(exist = false)
    private String city;
    
    /**
     * 积分
     */
    private Integer points;
    
    /**
     * 总游戏次�?     */
    @TableField(exist = false)
    private Integer totalGames;
    
    /**
     * 总消费金�?     */
    @TableField(exist = false)
    private java.math.BigDecimal totalSpend;
    
    /**
     * 最后登录时�?     */
    private LocalDateTime lastLoginTime;
    
    /**
     * 状态：1启用�?禁用
     */
    private Integer status;
    
    /**
     * 用户角色：USER-普通用户；SUPER_ADMIN-总部；STORE_ADMIN-门店管理员
     */
    private String role;

    /**
     * 管理员所属门店ID（SUPER_ADMIN 可为空）
     */
    private Long storeId;
    
    /**
     * 逻辑删除�?删除�?未删�?     */
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
