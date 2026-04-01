package com.murder.common.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 支付宝配置
 */
@Configuration
@ConfigurationProperties(prefix = "alipay")
@Data
public class AlipayConfig {

    // 应用ID（沙箱环境）
    private String appId = "2021000000000000";
    
    // 商户私钥
    private String privateKey = "your-private-key";
    
    // 支付宝公钥
    private String publicKey = "your-alipay-public-key";
    
    // 服务器异步通知页面路径
    private String notifyUrl = "http://localhost:8080/api/reservation/payment/notify";
    
    // 支付完成后支付宝同步回跳的后端地址
    private String returnUrl = "http://localhost:8080/api/reservation/payment/return";

    // 后端处理完成后重定向到前端结果页
    private String resultUrl = "http://localhost:3001/payment/result";
    
    // 请求网关地址（沙箱环境）
    private String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";
    
    // 编码格式
    private String charset = "UTF-8";
    
    // 返回格式
    private String format = "json";
    
    // 签名方式
    private String signType = "RSA2";
    
    // 是否使用模拟支付（开发测试用）
    private Boolean mockPayment = true;

    public String getSanitizedNotifyUrl() {
        return sanitizeUrl(notifyUrl);
    }

    public String getSanitizedReturnUrl() {
        return sanitizeUrl(returnUrl);
    }

    public String getSanitizedResultUrl() {
        return sanitizeUrl(resultUrl);
    }

    public String sanitizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        return url.replaceAll("\\s+", "").trim();
    }

    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                sanitizeUrl(gatewayUrl),
                appId,
                privateKey,
                format,
                charset,
                publicKey,
                signType
        );
    }
}

