package com.murder.common.context;

/**
 * 线程本地变量工具类，用于保存当前登录用户信息
 */
public class BaseContext {

    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    private static ThreadLocal<String> roleThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<Long> storeIdThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<Long> employeeIdThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<String> staffRoleThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<String> permissionCodesThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<Long> dmIdThreadLocal = new ThreadLocal<>();

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

    public static void setEmployeeId(Long employeeId) {
        employeeIdThreadLocal.set(employeeId);
    }

    public static Long getEmployeeId() {
        return employeeIdThreadLocal.get();
    }

    public static void setStaffRole(String staffRole) {
        staffRoleThreadLocal.set(staffRole);
    }

    public static String getStaffRole() {
        return staffRoleThreadLocal.get();
    }

    public static void setPermissionCodes(String permissionCodes) {
        permissionCodesThreadLocal.set(permissionCodes);
    }

    public static String getPermissionCodes() {
        return permissionCodesThreadLocal.get();
    }

    public static void setDmId(Long dmId) {
        dmIdThreadLocal.set(dmId);
    }

    public static Long getDmId() {
        return dmIdThreadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
        roleThreadLocal.remove();
        storeIdThreadLocal.remove();
        employeeIdThreadLocal.remove();
        staffRoleThreadLocal.remove();
        permissionCodesThreadLocal.remove();
        dmIdThreadLocal.remove();
    }
}
