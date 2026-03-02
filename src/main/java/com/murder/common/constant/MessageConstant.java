package com.murder.common.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    // 通用消息
    public static final String SUCCESS = "操作成功";
    public static final String FAILED = "操作失败";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String PARAM_ERROR = "参数错误";
    
    // 用户相关
    public static final String USER_NOT_FOUND = "用户不存在";
    public static final String USER_ALREADY_EXISTS = "用户已存在";
    public static final String PASSWORD_ERROR = "密码错误";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String ACCOUNT_LOCKED = "账号已被锁定";
    public static final String ACCOUNT_NOT_LOGIN = "账号未登录";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String USER_REGISTER_SUCCESS = "用户注册成功";
    
    // 门店相关
    public static final String STORE_NOT_FOUND = "门店不存在";
    public static final String STORE_ALREADY_EXISTS = "门店已存在";
    public static final String STORE_ROOM_NOT_FOUND = "房间不存在";
    public static final String STORE_CLOSED = "门店已打烊";
    
    // 剧本相关
    public static final String SCRIPT_NOT_FOUND = "剧本不存在";
    public static final String SCRIPT_ALREADY_EXISTS = "剧本已存在";
    public static final String SCRIPT_CATEGORY_NOT_FOUND = "剧本分类不存在";
    
    // 预约相关
    public static final String RESERVATION_NOT_FOUND = "预约不存在";
    public static final String RESERVATION_TIME_CONFLICT = "预约时间冲突";
    public static final String RESERVATION_ALREADY_CANCELLED = "预约已取消";
    public static final String RESERVATION_CANNOT_CANCEL = "预约不可取消";
    public static final String RESERVATION_FULL = "预约已满";
    
    // Token相关
    public static final String TOKEN_INVALID = "Token无效";
    public static final String TOKEN_EXPIRED = "Token已过期";
    public static final String TOKEN_MISSING = "Token缺失";
    
    // 文件相关
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String FILE_TYPE_ERROR = "文件类型错误";
}
