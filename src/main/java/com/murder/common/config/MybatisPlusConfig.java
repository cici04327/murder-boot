package com.murder.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置类
 */
@Configuration
@Slf4j
public class MybatisPlusConfig {

    /**
     * 分页插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("配置 MyBatis Plus 分页插件");
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 创建分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置最大单页限制数量
        paginationInterceptor.setMaxLimit(1000L);
        // 设置溢出处理
        paginationInterceptor.setOverflow(false);
        
        // 添加到拦截器
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        log.info("MyBatis Plus 分页插件配置完成");
        
        return interceptor;
    }
    
    /**
     * 自定义 MyBatis-Plus 配置，禁用 SqlRunner
     */
    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusPropertiesCustomizer() {
        return properties -> {
            // 禁用 SqlRunner
            properties.getGlobalConfig().setEnableSqlRunner(false);
            log.info("已禁用 MyBatis-Plus SqlRunner");
        };
    }
}
