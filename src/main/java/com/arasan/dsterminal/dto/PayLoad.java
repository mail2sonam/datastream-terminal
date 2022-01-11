package com.arasan.dsterminal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayLoad {
    private String payLoadId;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="Asia/Kolkata")
    private LocalDateTime msgPostedOn;

    private String source;

    private String topicName;
    private String destSubscriberId;

    private Object message;

    @JsonIgnore
    private LocalDateTime msgReceivedOn;

    public String getRegId(String tenantId){
        return tenantId+":"+topicName+":"+destSubscriberId;
    }

    @Override
    public String toString() {
        return "PayLoad{" +
                "payLoadId='" + payLoadId + '\'' +
                ", msgPostedOn=" + msgPostedOn +
                ", topicName='" + topicName + '\'' +
                ", destSubscriberId='" + destSubscriberId + '\'' +
                '}';
    }
}
