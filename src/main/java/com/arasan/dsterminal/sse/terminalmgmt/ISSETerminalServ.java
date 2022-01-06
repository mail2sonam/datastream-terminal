package com.arasan.dsterminal.sse.terminalmgmt;

import com.arasan.dsterminal.dto.PayLoad;

import java.util.List;

public interface ISSETerminalServ {

    SSEEmitterWrapper provisionUserSSE(String accountId, String topicName, String subscriberId,String requestedIpAddress,String userAgent);
    void sendMsg(PayLoad payLoad);
    void logOutUser(String accountId, String topicName, String subscriberId);
    List<String> getAllConnectionStats();
}
