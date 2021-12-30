package com.arasan.dsterminal.sse.terminalmgmt;

import java.io.IOException;
import java.util.Date;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class SSEEmitterWrapper extends SseEmitter{
	
	public SSEEmitterWrapper(long timeOut, String regId, String regHost){
		super(timeOut);
		this.regId =regId;
		this.regHost = regHost;
	}
	
	private String regId;
	private String regHost;
	private long lastMsgOn;

	public long getLastMsgOn() {
		return lastMsgOn;
	}

	public String getRegId() {
		return regId;
	}

	public String getRegHost() {
		return regHost;
	}

	@Override
	public void send(Object object, @Nullable MediaType mediaType) throws IOException {
		super.send(event().data(object, mediaType));
		this.lastMsgOn = System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return "SSEEmitterWrapper{" +
				"regId='" + regId + '\'' +
				", regHost='" + regHost + '\'' +
				", lastMsgOn=" + lastMsgOn +
				'}';
	}
}
