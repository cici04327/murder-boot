package com.murder.common.config;

import com.murder.interceptor.JwtTokenInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private JwtTokenInterceptor jwtTokenInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Register JWT interceptor");
        
        registry.addInterceptor(jwtTokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    // ========== 认证相关 ==========
                    "/api/user/login",
                    "/api/user/register",
                    "/api/user/admin/login",
                    "/api/store/login",
                    "/api/auth/**",
                    
                    // ========== 第三方支付回调（公开） ==========
                    "/api/reservation/payment/notify",
                    "/api/reservation/payment/return",
                    
                    // ========== 剧本相关（公开） ==========
                    "/api/script/page",
                    "/api/script/list/**",
                    "/api/script/*/detail",
                    "/api/script/*/roles",
                    "/api/script/category",
                    "/api/script/category/**",
                    "/api/script/review/page",
                    "/api/script/review/*",
                    "/api/script/favorite/*/status",
                    
                    // ========== 门店相关（公开） ==========
                    "/api/store/page",
                    "/api/store/list",
                    "/api/store/list/**",
                    "/api/store/*/detail",
                    "/api/store/detail/*",
                    "/api/store/statistics",
                    "/api/store/review/page",
                    
                    // ========== 文章相关（公开） ==========
                    "/api/article/page",
                    "/api/article/list/**",
                    "/api/article/*/detail",
                    "/api/article/*/comments",
                    "/api/article/hot",
                    "/api/article/recommended",
                    "/api/article/*/favorite/status",
                    
                    // ========== 统计相关（公开） ==========
                    "/api/statistics/overview",
                    "/api/statistics/charts",
                    "/api/statistics/rankings",
                    "/api/statistics/realtime",
                    
                    // ========== 推荐相关（公开） ==========
                    "/api/recommendation/personalized",
                    "/api/recommendation/hot",
                    "/api/recommendation/new",
                    "/api/recommendation/history",
                    "/api/recommendation/collaborative/*",
                    "/api/recommendation/similar/*",
                    
                    // ========== WebSocket ==========
                    "/api/ws/**",
                    
                    // ========== 留言反馈（公开） ==========
                    "/api/feedback/submit",
                    
                    // ========== AI智能客服（公开） ==========
                    "/api/ai/**",
                    
                    // ========== API文档 ==========
                    "/doc.html",
                    "/swagger-resources/**",
                    "/v3/api-docs/**",
                    "/webjars/**",
                    "/favicon.ico"
                );
    }
    
    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("Register static resource handlers");
        
        // 上传文件访问路径映射（转换为绝对路径并规范化）
        java.nio.file.Path absoluteUploadPath = java.nio.file.Paths.get(uploadPath).toAbsolutePath().normalize();
        String uploadLocation = "file:" + absoluteUploadPath.toString().replace("\\", "/") + "/";
        log.info("Upload resource location: {}", uploadLocation);
        log.info("Upload path exists: {}", java.nio.file.Files.exists(absoluteUploadPath));
        registry.addResourceHandler("/upload/**", "/uploads/**")
                .addResourceLocations(uploadLocation);
        
        // 用户前端静态资源
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/user/")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/public/");
        
        // 管理后台静态资源
        registry.addResourceHandler("/admin/**")
                .addResourceLocations("classpath:/static/admin/");
    }
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 首页重定向
        registry.addViewController("/").setViewName("forward:/index.html");
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("Register CORS mappings");
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
