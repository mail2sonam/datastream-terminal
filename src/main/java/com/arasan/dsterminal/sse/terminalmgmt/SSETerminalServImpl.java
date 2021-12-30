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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

@Service
public class SSETerminalServImpl implements ISSETerminalServ {

    public static final Logger log = LoggerFactory.getLogger(SSETerminalServImpl.class);

	public SSETerminalServImpl(ICache sseConnectionCache){
		this.sseConnectionCache = sseConnectionCache;
	}

	private ICache sseConnectionCache;

	@Override
	public SSEEmitterWrapper provisionUserSSE(String accountId, String topicName, String subscriberId) {
		if(!StringUtils.hasLength(accountId)
				|| !StringUtils.hasLength(topicName)
				|| !StringUtils.hasLength(subscriberId))
			{
			log.info("Quitting because empty accountId=%s,topicName=%s,subscriberId=%s",
					accountId,topicName,subscriberId);
			return null;
			}
		String regId = getRegId(accountId,topicName,subscriberId);
		SSEEmitterWrapper sseEmitter = new SSEEmitterWrapper(Long.MAX_VALUE,regId,currentNodeHostWithPort);
		sseEmitter.onCompletion(() -> log.info(sseEmitter.getRegId()+" is completed!"));
    	sseEmitter.onTimeout(() -> log.info(sseEmitter.getRegId()+" timedOut!"));
    	sseEmitter.onError((ex) -> log.info("Error in "+sseEmitter.getRegId(), ex));
		sseConnectionCache.add(regId,sseEmitter);
    	return sseEmitter;
	}


	@Override
	public void sendMsg(PayLoad obj) {
		if(obj==null
				|| !StringUtils.hasLength(obj.getAccountId())
				|| !StringUtils.hasLength(obj.getTopicName())
				|| !StringUtils.hasLength(obj.getDestSubscriberId()))
		{
			log.info("Skipping payload due to payload being null or destination null:%s"
					,(obj==null ?null:(obj.getAccountId())+"::"+obj.getTopicName()+"::"+obj.getDestSubscriberId()));
			return;
		}
		String regId = obj.getRegId();
		Set<SSEEmitterWrapper> regSet = sseConnectionCache.get(regId);
		if(regSet==null || regSet.isEmpty()) {
			log.info("Skipping Posting as no subscribers found for :%s",regId);
			return;
		}
		regSet.forEach(sseEmitter -> {
			log.debug("Posting to subscriber %s msg %s",regId,obj);
			if(currentNodeHostWithPort.equals(sseEmitter.getRegHost())){
				postObjectToSSE(sseEmitter, obj);
			}else{
				log.debug("Skipping Posting as hostname is %s for %s,but host derived is %s"
						,sseEmitter.getRegHost(),regId,currentNodeHostWithPort);
			}
		});
		log.debug("Posted %d messages to subscriber %s",regSet.size(),regId);
	}
	
	
	private void postObjectToSSE(SSEEmitterWrapper sse, PayLoad obj) {
		try {
			sse.send(obj,null);
		} catch (IOException e1) {
			sse.completeWithError(e1);
			log.error("Instructing Removal of SSE due to IOError->"+obj.getRegId(),e1);
			sseConnectionCache.remove(sse);
		}
	}

	@Override
	public void logOutUser(String accountId, String topicName, String subscriberId) {
		String regId = getRegId(accountId,topicName,subscriberId);
		Optional.ofNullable(sseConnectionCache.get(regId))
				.orElse(Collections.emptySet())
				.forEach(sse -> sse.complete());
		sseConnectionCache.remove(regId);
	}

	private String getRegId(String accountId,String topicName,String destSubscriberId){
		return accountId+"::"+topicName+"::"+destSubscriberId;
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
}
