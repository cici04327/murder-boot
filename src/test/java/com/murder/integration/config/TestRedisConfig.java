package com.murder.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ZSetOperations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 测试环境 Redis 配置
 * 使用 Mock 对象替代真实的 Redis 连接
 */
@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        RedisStringCommands stringCommands = mock(RedisStringCommands.class);
        org.springframework.data.redis.connection.RedisKeyCommands keyCommands =
                mock(org.springframework.data.redis.connection.RedisKeyCommands.class);
        when(factory.getConnection()).thenReturn(connection);
        when(connection.stringCommands()).thenReturn(stringCommands);
        when(connection.keyCommands()).thenReturn(keyCommands);
        // mock del 方法，返回0L表示删除0个key
        when(keyCommands.del(org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        return factory;
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        ListOperations<String, Object> listOperations = mock(ListOperations.class);
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        // mock get 返回 null（缓存未命中）
        when(valueOperations.get(anyString())).thenReturn(null);
        // mock increment 返回 0L
        when(valueOperations.increment(anyString())).thenReturn(0L);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(0L);
        when(valueOperations.decrement(anyString())).thenReturn(0L);
        // mock set 操作（void，默认 doNothing）
        doNothing().when(valueOperations).set(anyString(), any());
        doNothing().when(valueOperations).set(anyString(), any(), any());
        doNothing().when(valueOperations).set(anyString(), any(),
                org.mockito.ArgumentMatchers.anyLong(), any());
        // mock hasKey / delete / expire
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.delete(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(0L);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);
        when(redisTemplate.getExpire(anyString())).thenReturn(-1L);
        // mock set operations
        when(setOperations.members(anyString())).thenReturn(new java.util.HashSet<>());
        when(setOperations.add(anyString(), any())).thenReturn(0L);
        // mock list operations
        when(listOperations.size(anyString())).thenReturn(0L);
        when(listOperations.range(anyString(), anyLong(), anyLong()))
                .thenReturn(new java.util.ArrayList<>());
        // mock hash operations
        when(hashOperations.get(anyString(), any())).thenReturn(null);
        when(hashOperations.entries(anyString())).thenReturn(new java.util.HashMap<>());

        return redisTemplate;
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        ListOperations<String, String> listOperations = mock(ListOperations.class);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.increment(anyString())).thenReturn(0L);
        when(valueOperations.decrement(anyString())).thenReturn(0L);
        doNothing().when(valueOperations).set(anyString(), anyString());
        doNothing().when(valueOperations).set(anyString(), anyString(), any());
        doNothing().when(valueOperations).set(anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyLong(), any());
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);
        when(stringRedisTemplate.delete(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(0L);
        when(stringRedisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        return stringRedisTemplate;
    }
}
