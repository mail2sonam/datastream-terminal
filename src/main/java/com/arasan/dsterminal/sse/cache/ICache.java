package com.arasan.dsterminal.sse.cache;

import com.arasan.dsterminal.sse.terminalmgmt.SSEEmitterWrapper;

import java.time.Duration;
import java.util.Set;

public interface ICache {
    Set<SSEEmitterWrapper> get(String key);
    void add(String key, SSEEmitterWrapper emitterWrapper);
    void remove(SSEEmitterWrapper emitterWrapper);
    void remove(String key);
    Set<SSEEmitterWrapper> getIdle(Duration duration);
}
