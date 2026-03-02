package com.murder.common.constant;

/**
 * Redis常量
 */
public class RedisConstant {

    /**
     * 门店缓存key前缀
     */
    public static final String STORE_CACHE_PREFIX = "store:";
    
    /**
     * 用户缓存key前缀
     */
    public static final String USER_CACHE_PREFIX = "user:";
    
    /**
     * 剧本缓存key前缀
     */
    public static final String SCRIPT_CACHE_PREFIX = "script:";
    
    /**
     * 预约缓存key前缀
     */
    public static final String RESERVATION_CACHE_PREFIX = "reservation:";
    
    /**
     * 限流key前缀
     */
    public static final String RATE_LIMIT_PREFIX = "rate_limit:";
    
    /**
     * 缓存过期时间（秒）
     */
    public static final long CACHE_EXPIRE_TIME = 1800; // 30分钟
    
    /**
     * 热点数据缓存过期时间（秒）
     */
    public static final long HOT_DATA_EXPIRE_TIME = 3600; // 1小时
    
    // ==================== 新增缓存Key常量 ====================
    
    /**
     * 热门剧本缓存Key
     */
    public static final String HOT_SCRIPTS_KEY = "hot:scripts";
    
    /**
     * 推荐剧本缓存Key
     */
    public static final String RECOMMENDED_SCRIPTS_KEY = "recommended:scripts";
    
    /**
     * 剧本分类缓存Key
     */
    public static final String SCRIPT_CATEGORIES_KEY = "script:categories";
    
    /**
     * 热门门店缓存Key
     */
    public static final String HOT_STORES_KEY = "hot:stores";
    
    /**
     * 首页数据缓存Key
     */
    public static final String HOME_DATA_KEY = "home:data";
    
    /**
     * 文章列表缓存Key前缀
     */
    public static final String ARTICLE_LIST_PREFIX = "article:list:";
    
    // ==================== 缓存过期时间配置 ====================
    
    /**
     * 列表数据缓存过期时间（秒）- 10分钟
     */
    public static final long LIST_CACHE_EXPIRE_TIME = 600;
    
    /**
     * 详情数据缓存过期时间（秒）- 30分钟
     */
    public static final long DETAIL_CACHE_EXPIRE_TIME = 1800;
    
    /**
     * 分类数据缓存过期时间（秒）- 2小时
     */
    public static final long CATEGORY_CACHE_EXPIRE_TIME = 7200;
    
    /**
     * 首页数据缓存过期时间（秒）- 5分钟
     */
    public static final long HOME_CACHE_EXPIRE_TIME = 300;
}
