package com.arasan.dsterminal.dto;

import lombok.Data;

import java.util.Date;

@Data
public class PayLoad {
    private String payLoadId;
    private Date msgPostedOn;

    private String source;

    private String accountId;
    private String topicName;
    private String destSubscriberId;

    private Object message;

    public String getRegId(){
        return accountId+"::"+topicName+"::"+destSubscriberId;
    }
}
