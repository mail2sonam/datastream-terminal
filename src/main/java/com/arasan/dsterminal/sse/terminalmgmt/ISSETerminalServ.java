package com.arasan.dsterminal.sse.terminalmgmt;

import com.arasan.dsterminal.dto.PayLoad;

import java.util.List;

public interface ISSETerminalServ {

    SSEEmitterWrapper provisionUserSSE(String tenantId, String topicName, String subscriberId,String requestedIpAddress,String userAgent);
    void sendMsg(String tenantId, PayLoad payLoad);
    void logOutUser(String tenantId, String topicName, String subscriberId);
    List<String> getAllConnectionStats();
}
