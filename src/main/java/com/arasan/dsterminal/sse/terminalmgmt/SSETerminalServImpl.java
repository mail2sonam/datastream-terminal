package com.arasan.dsterminal.sse.terminalmgmt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import com.arasan.dsterminal.sse.cache.ICache;
import com.arasan.dsterminal.dto.PayLoad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;

@Service
public class SSETerminalServImpl implements ISSETerminalServ {

    public static final Logger log = LoggerFactory.getLogger(SSETerminalServImpl.class);

	public SSETerminalServImpl(ICache sseConnectionCache){
		this.sseConnectionCache = sseConnectionCache;
	}

	private ICache sseConnectionCache;

	@Autowired
	Tracer tracer;

	@Override
	public SSEEmitterWrapper provisionUserSSE(String tenantId, String topicName, String subscriberId
			,String requestedIpAddress,String userAgent) {
		if(!StringUtils.hasLength(tenantId)
				|| !StringUtils.hasLength(topicName)
				|| !StringUtils.hasLength(subscriberId))
			{
			log.info("Quitting because empty accountId={},topicName={},subscriberId={},ip={},useragent={}",
					tenantId,topicName,subscriberId,requestedIpAddress,userAgent);
			return null;
			}
		String regId = SSETerminalUtils.getRegId(tenantId,topicName,subscriberId);
		SSEEmitterWrapper sseEmitter = new SSEEmitterWrapper(Long.MAX_VALUE,regId,currentNodeHostWithPort
				,requestedIpAddress,userAgent);
		sseEmitter.onCompletion(() -> {
			log.info(sseEmitter.getRegId()+" is completed!");
			sseConnectionCache.remove(sseEmitter);
		});
    	sseEmitter.onTimeout(() -> {
			log.info(sseEmitter.getRegId() + " timedOut!");
			sseConnectionCache.remove(sseEmitter);
			});
    	sseEmitter.onError((ex) -> {
			log.info("Error in "+sseEmitter.getRegId()+":"+ex.getMessage());
			sseConnectionCache.remove(sseEmitter);
			});
		sseConnectionCache.add(regId,sseEmitter);
    	return sseEmitter;
	}


	@Override
	public String sendMsg(String tenantId,PayLoad obj) {
		if(obj==null
				|| !StringUtils.hasLength(obj.getTopicName())
				|| !StringUtils.hasLength(obj.getDestSubscriberId()))
		{
			log.info("Skipping payload due to payload being null or destination null:{}"
					,(obj==null ?null:(obj.getTopicName()+"::"+obj.getDestSubscriberId())));
			throw new IllegalArgumentException("Payload or Destination Null");
		}
		String traceId = tracer.currentSpan().context().traceId();
		String regId = obj.getRegId(tenantId);
		Set<SSEEmitterWrapper> regSet = sseConnectionCache.get(regId);
		if(regSet==null || regSet.isEmpty()) {
			log.info("Skipping Posting as no subscribers found for :{}",regId);
			return traceId;
		}
		regSet.forEach(sseEmitter -> {
			log.debug("Posting to subscriber {} msg {}",regId,obj);
			if(currentNodeHostWithPort.equals(sseEmitter.getRegHost())){
				postObjectToSSE(sseEmitter, obj);
			}else{
				log.debug("Skipping Posting as hostname is {} for {},but host derived is {}"
						,sseEmitter.getRegHost(),regId,currentNodeHostWithPort);
			}
		});
		log.debug("Posted to {} connections for subscriber {}",regSet.size(),regId);
		return traceId;
	}
	
	
	private void postObjectToSSE(SSEEmitterWrapper sse, PayLoad obj) {
		try {
			SseEmitter.SseEventBuilder builder = SseEmitter.event()
					.data(obj, MediaType.APPLICATION_JSON)
					.id(obj.getPayLoadId())
					.name("cti-event");
					//.reconnectTime(10_000L);
			sse.send(builder);
		} catch (IOException e1) {
			log.error(sse.getRegId()+"Remove:IOException---->"+e1);
			sseConnectionCache.remove(sse);
			sse.completeWithError(e1);
		}
	}

	@Override
	public void logOutUser(String accountId, String topicName, String subscriberId) {
		String regId = SSETerminalUtils.getRegId(accountId,topicName,subscriberId);
		Optional.ofNullable(sseConnectionCache.get(regId))
				.orElse(Collections.emptySet())
				.forEach(sse -> sse.complete());
		sseConnectionCache.remove(regId);
	}

	ScheduledExecutorService scheduledExecutorService =
			Executors.newScheduledThreadPool(1);

	ScheduledFuture scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
			() -> {
				log.info("Emitter Purge thread starting");
				Set<SSEEmitterWrapper> idleSSE = sseConnectionCache.getIdle(Duration.ofMinutes(5));
				idleSSE
					.forEach(e -> {
						log.info("Posting Test Msg to Emitter:"+e.toString());
						postObjectToSSE(e, new PayLoad());
					});
				log.info("Emitter Purge thread ending");
			},10,10,TimeUnit.MINUTES);

	private String currentNodeHostWithPort;

	@PostConstruct
	private void init() throws UnknownHostException {
		this.setCurrentNodeWithPort();
	}

	@Autowired
	Environment environment;

	private void setCurrentNodeWithPort() throws UnknownHostException {
		String aPort = environment.getProperty("local.server.port");
		currentNodeHostWithPort=aPort+":"+ InetAddress.getLocalHost().getHostAddress()+"::"+InetAddress.getLocalHost().getHostName();
	}

	@Override
	public List<String> getAllConnectionStats(){
		return sseConnectionCache.getAllConnectionStats();
	}
}
