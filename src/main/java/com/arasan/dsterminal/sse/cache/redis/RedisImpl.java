package com.arasan.dsterminal.sse.cache.redis;

import com.arasan.dsterminal.sse.cache.ICache;
import com.arasan.dsterminal.sse.terminalmgmt.SSEEmitterWrapper;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Profile("redis")
@Component
public class RedisImpl implements ICache {

    ReactiveRedisTemplate<String,SSEEmitterWrapper> reactiveRedisTemplate;
    ReactiveHashOperations hashOperations;

    RedisImpl(ReactiveRedisTemplate<String,SSEEmitterWrapper> reactiveRedisTemplate){
        this.reactiveRedisTemplate=reactiveRedisTemplate;
        this.hashOperations = this.reactiveRedisTemplate.opsForHash();
    }

    @Override
    public Set<SSEEmitterWrapper> get(String key) {
        return (Set<SSEEmitterWrapper>)hashOperations.values(key).collect(Collectors.toSet()).block();
    }

    @Override
    public void add(String key, SSEEmitterWrapper emitterWrapper) {
        return;
    }

    @Override
    public void remove(SSEEmitterWrapper emitterWrapper) {
        return;
    }

    @Override
    public void remove(String key) {
        return;
    }

    @Override
    public Set<SSEEmitterWrapper> getIdle(Duration duration) {
        return null;
    }

    @Override
    public List<String> getAllConnectionStats() {
        return null;
    }
}
