package com.arasan.dsterminal.sse.cache.redis;

import com.arasan.dsterminal.sse.terminalmgmt.SSEEmitterWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Profile("redis")
@Configuration
public class RedisConfig {

    private final String HOSTNAME;

    private final int PORT;

    private final int DATABASE;

    private final String PASSWORD;

    private final long TIMEOUT;

    public RedisConfig(
            @Value("${redis.hostname}") String hostname,
            @Value("${redis.port}") int port,
            @Value("${redis.database}") int database,
            @Value("${redis.password}") String password,
            @Value("${redis.timeout}") long timeout
    ) {

        this.HOSTNAME = hostname;
        this.PORT = port;
        this.DATABASE = database;
        this.PASSWORD = password;
        this.TIMEOUT = timeout;
    }

    @Bean
    public ReactiveRedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(HOSTNAME);
        config.setPort(PORT);
        config.setDatabase(DATABASE);
        config.setPassword(PASSWORD);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(TIMEOUT))
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public ReactiveRedisTemplate<String, SSEEmitterWrapper> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<SSEEmitterWrapper> valueSerializer =
                new Jackson2JsonRedisSerializer<>(SSEEmitterWrapper.class);
        RedisSerializationContext.RedisSerializationContextBuilder<String, SSEEmitterWrapper> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
        RedisSerializationContext<String, SSEEmitterWrapper> context =
                builder.value(valueSerializer).build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
