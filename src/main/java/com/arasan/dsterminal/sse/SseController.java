package com.arasan.dsterminal.sse;

import com.arasan.dsterminal.dto.PayLoad;
import com.arasan.dsterminal.sse.terminalmgmt.ISSETerminalServ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/stream")
public class SseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseController.class);

    ISSETerminalServ sseService;

    public SseController(ISSETerminalServ sseService){
        this.sseService=sseService;
    }

    @GetMapping(path = "/{accountId}/{topicName}/{subscriberId}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    public SseEmitter streamData(@PathVariable(value="accountId") String accountId,
                                 @PathVariable(value="topicName") String topicName,
                                 @PathVariable(value="subscriberId") String subscriberId) {
    	return sseService.provisionUserSSE(accountId,topicName,subscriberId);
    }

    @PostMapping
    @CrossOrigin
    public void postData(@RequestBody PayLoad data) {
    	sseService.sendMsg(data);
    }

    @GetMapping(path = "/logout/{accountId}/{topicName}/{subscriberId}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    public void logout(@PathVariable(value="accountId") String accountId,
                                 @PathVariable(value="topicName") String topicName,
                                 @PathVariable(value="subscriberId") String subscriberId) {
        sseService.logOutUser(accountId,topicName,subscriberId);
    }


}