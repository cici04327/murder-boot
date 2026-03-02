package com.murder.common.constant;

/**
 * 状态常量类
 */
public class StatusConstant {

    // 启用状态
    public static final Integer ENABLE = 1;
    public static final Integer DISABLE = 0;
    
    // 逻辑删除
    public static final Integer DELETED = 1;
    public static final Integer NOT_DELETED = 0;
    
    // 预约状态
    public static final Integer RESERVATION_PENDING = 1;    // 待确认
    public static final Integer RESERVATION_CONFIRMED = 2;  // 已确认
    public static final Integer RESERVATION_COMPLETED = 3;  // 已完成
    public static final Integer RESERVATION_CANCELLED = 4;  // 已取消
    
    // 支付状态
    public static final Integer UNPAID = 0;  // 未支付
    public static final Integer PAID = 1;    // 已支付
    public static final Integer REFUNDED = 2; // 已退款
}
