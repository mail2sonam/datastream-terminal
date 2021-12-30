package com.arasan.dsterminal.sse.cache.inmemory;

import com.arasan.dsterminal.sse.cache.ICache;
import com.arasan.dsterminal.sse.terminalmgmt.SSEEmitterWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Component
public class InMemoryImpl implements ICache {

    Logger log = LoggerFactory.getLogger(this.getClass().getName());
    //Map<"accountId:userId:queueName",Set<sseEmitter>>
    private Map<String,Set<SSEEmitterWrapper>> localSubscriberSSEMap = new ConcurrentHashMap<>();

    @Override
    public Set<SSEEmitterWrapper> get(String key) {
        return localSubscriberSSEMap.get(key);
    }

    @Override
    public void add(String key, SSEEmitterWrapper sseEmitter) {
        Set<SSEEmitterWrapper> userSet = localSubscriberSSEMap.getOrDefault(key,new CopyOnWriteArraySet<SSEEmitterWrapper>());
        userSet.add(sseEmitter);
        localSubscriberSSEMap.put(key, userSet);
        log.info("Added new SSEEmitter for User:"+key);
    }

    @Override
    public void remove(SSEEmitterWrapper emitterWrapper) {
        String key=emitterWrapper.getRegId();
        Set<SSEEmitterWrapper> emitterSet = localSubscriberSSEMap.get(key);
        Optional.ofNullable(emitterSet)
            .orElse(Collections.emptySet())
            .remove(emitterWrapper);

        if(emitterSet!=null && emitterSet.isEmpty()){
            this.remove(key);
        }
    }

    @Override
    public void remove(String key) {
        Optional.ofNullable(localSubscriberSSEMap.get(key))
                .orElse(Collections.emptySet())
                .clear();
        localSubscriberSSEMap.remove(key);
    }

    @Override
    public Set<SSEEmitterWrapper> getIdle(Duration duration) {
        long threshHoldMillis = System.currentTimeMillis() - duration.toMillis();
        return localSubscriberSSEMap.values().stream().flatMap(Collection::stream)
                .filter(e -> e!=null && e.getLastMsgOn() < threshHoldMillis)
                .collect(Collectors.toSet());
    }
}
