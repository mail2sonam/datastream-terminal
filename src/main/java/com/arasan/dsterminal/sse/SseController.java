package com.arasan.dsterminal.sse;

import com.arasan.dsterminal.dto.PayLoad;
import com.arasan.dsterminal.sse.terminalmgmt.ISSETerminalServ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

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
    public SseEmitter streamData(@RequestHeader(name = "Last-Event-ID", required = false) String lastId,
                                 @RequestHeader(value = "User-Agent") String userAgent,
                                 @PathVariable(value="accountId") String accountId,
                                 @PathVariable(value="topicName") String topicName,
                                 @PathVariable(value="subscriberId") String subscriberId,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        String ipAddress = request.getRemoteAddr();
        response.setHeader("Cache-Control", "no-store");
        return sseService.provisionUserSSE(accountId,topicName,subscriberId,ipAddress,userAgent);
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


    @GetMapping(path = "/getAllConnectionStats")
    public List<String> getAllConnectionStats() {
        return sseService.getAllConnectionStats();
    }



}