package com.murder;

import com.murder.common.properties.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 剧本杀预约与门店管理系统 - Spring Boot 单体应用启动类
 * 
 * 整合了原有的微服务架构：
 * - murder-auth: 认证服务
 * - 用户模块：登录注册、积分、优惠券、通知等
 * - 门店模块：门店信息、房间、员工、统计等
 * - 剧本模块：剧本、分类、角色、收藏、评价等
 * - murder-reservation: 预约服务
 * 
 * @author Murder System
 * @date 2025-01-15
 */
@Slf4j
@SpringBootApplication
@MapperScan("com.murder.mapper")
@EnableCaching
@EnableScheduling
@EnableTransactionManagement
@EnableConfigurationProperties(JwtProperties.class)
public class MurderBootApplication {

    public static void main(String[] args) throws UnknownHostException {
        log.info("=== 开始启动 Spring Boot 应用 ===");
        ConfigurableApplicationContext application = SpringApplication.run(MurderBootApplication.class, args);
        
        Environment env = application.getEnvironment();
        String port = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path", "/");
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        
        log.info("\n========================================\n" +
                "剧本杀预约与门店管理系统启动成功！\n" +
                "本地访问: \thttp://localhost:{}{}\n" +
                "外部访问: \thttp://{}:{}{}\n" +
                "API文档: \thttp://localhost:{}{}/doc.html\n" +
                "Druid监控: \thttp://localhost:{}{}/druid\n" +
                "========================================",
                port, contextPath,
                hostAddress, port, contextPath,
                port, contextPath,
                port, contextPath);
    }
}
