package com.murder.common.context;

/**
 * 线程本地变量工具类，用于保存当前登录用户信息
 */
public class BaseContext {

    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    private static ThreadLocal<String> roleThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<Long> storeIdThreadLocal = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    /**
     * 移除当前用户ID
     */
    public static void setRole(String role) {
        roleThreadLocal.set(role);
    }

    public static String getRole() {
        return roleThreadLocal.get();
    }

    public static void setStoreId(Long storeId) {
        storeIdThreadLocal.set(storeId);
    }

    public static Long getStoreId() {
        return storeIdThreadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
        roleThreadLocal.remove();
        storeIdThreadLocal.remove();
    }
}
