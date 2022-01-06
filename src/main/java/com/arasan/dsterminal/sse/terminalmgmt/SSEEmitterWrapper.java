package com.arasan.dsterminal.sse.terminalmgmt;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ToString
public class SSEEmitterWrapper extends SseEmitter{

	@ToString.Exclude
	Logger logger = LoggerFactory.getLogger(SSEEmitterWrapper.class);

	public SSEEmitterWrapper(long timeOut, String regId, String regHost,String requestedFromIp,String userAgent){
		super(timeOut);
		this.id= UUID.randomUUID().toString();
		this.regId =regId;
		this.regHost = regHost;
		this.requestedFromIp=requestedFromIp;
		this.userAgent=userAgent;
		this.createdOn = System.currentTimeMillis();
	}

	@Getter
	private String id;

	@ToString.Exclude
	private long createdOn;

	@Getter
	private String regId;

	@Getter
	private String regHost;

	@Getter
	@ToString.Exclude
	private long lastMsgOn;

	private String requestedFromIp;
	private String userAgent;

	@Override
	public void send(SseEmitter.SseEventBuilder builder) throws IOException{
		super.send(builder);
		this.lastMsgOn = System.currentTimeMillis();
		logger.debug(regId+":lastMsgOn->{}",this.lastMsgOn);
	}

	@Override
	public void send(Object object, @Nullable MediaType mediaType) throws IOException {
		super.send(event().data(object, mediaType));
		this.lastMsgOn = System.currentTimeMillis();
		logger.debug(regId+":lastMsgOn->{}",this.lastMsgOn);
	}

	@ToString.Include
	public String lastMsgOn() {
		return String.valueOf(lastMsgOn>0?new Date(lastMsgOn):lastMsgOn);
	}

	@ToString.Include
	public String createdOn() {
		return String.valueOf(lastMsgOn>0?new Date(createdOn):createdOn);
	}
}
