package com.springai.auth.context;

import com.springai.auth.entity.User;

/**
 * 用户上下文工具类
 * 使用 ThreadLocal 存储当前登录用户信息
 * 
 * @author yanwenjie
 */
public class UserContext {
    
    private static final ThreadLocal<User> USER_CONTEXT = new ThreadLocal<>();
    
    /**
     * 私有构造函数，防止实例化
     */
    private UserContext() {
        throw new UnsupportedOperationException("工具类不能实例化");
    }
    
    /**
     * 设置当前用户
     * 
     * @param user 用户对象
     */
    public static void setUser(User user) {
        USER_CONTEXT.set(user);
    }
    
    /**
     * 获取当前用户
     * 
     * @return 用户对象，如果未登录则返回 null
     */
    public static User getUser() {
        return USER_CONTEXT.get();
    }
    
    /**
     * 获取当前用户手机号
     * 
     * @return 手机号，如果未登录则返回 null
     */
    public static String getPhoneNumber() {
        User user = getUser();
        return user != null ? user.getPhoneNumber() : null;
    }
    
    /**
     * 清除当前用户信息
     * 应在请求结束后调用，避免内存泄漏
     */
    public static void clear() {
        USER_CONTEXT.remove();
    }
    
    /**
     * 检查当前是否有用户登录
     * 
     * @return 如果已登录返回 true，否则返回 false
     */
    public static boolean isAuthenticated() {
        return getUser() != null;
    }
}

