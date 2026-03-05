package com.murder.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson配置
 */
@Configuration
public class JacksonConfig {

    /**
     * 日期时间格式
     */
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "HH:mm:ss";

    /**
     * 自定义 LocalTime 反序列化器，兼容 HH:mm 和 HH:mm:ss 两种格式
     */
    static class FlexibleLocalTimeDeserializer extends StdDeserializer<LocalTime> {
        FlexibleLocalTimeDeserializer() { super(LocalTime.class); }

        @Override
        public LocalTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String text = p.getText().trim();
            if (text.length() == 5) { // HH:mm
                return LocalTime.parse(text, DateTimeFormatter.ofPattern("HH:mm"));
            }
            return LocalTime.parse(text, DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 注册JavaTimeModule
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // LocalDateTime序列化和反序列化
        javaTimeModule.addSerializer(LocalDateTime.class, 
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        javaTimeModule.addDeserializer(LocalDateTime.class, 
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        
        // LocalDate序列化和反序列化
        javaTimeModule.addSerializer(LocalDate.class, 
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        javaTimeModule.addDeserializer(LocalDate.class, 
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        
        // LocalTime序列化和反序列化（序列化用 HH:mm:ss，反序列化兼容 HH:mm 和 HH:mm:ss）
        javaTimeModule.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_FORMAT)));
        javaTimeModule.addDeserializer(LocalTime.class, new FlexibleLocalTimeDeserializer());
        
        objectMapper.registerModule(javaTimeModule);
        
        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 反序列化时忽略未知字段
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        return objectMapper;
    }
}

