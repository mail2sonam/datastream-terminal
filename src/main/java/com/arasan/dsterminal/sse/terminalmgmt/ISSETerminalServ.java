package com.arasan.dsterminal.sse.terminalmgmt;

import com.arasan.dsterminal.dto.PayLoad;

public interface ISSETerminalServ {

    SSEEmitterWrapper provisionUserSSE(String accountId, String topicName, String subscriberId);
    void sendMsg(PayLoad payLoad);
    void logOutUser(String accountId, String topicName, String subscriberId);
}
