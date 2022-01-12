package com.arasan.dsterminal.sse.terminalmgmt;

public class SSETerminalUtils {
    public static String getRegId(String accountId,String topicName,String destSubscriberId){
        return accountId+"::"+topicName+"::"+destSubscriberId;
    }
}
