package com.arasan.dsterminal.sse;

import com.arasan.dsterminal.dto.PayLoad;
import com.arasan.dsterminal.sse.terminalmgmt.ISSETerminalServ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Secured("ROLE_datastream-consumer")
    @GetMapping(path = "/{tenantId}/{topicName}/{subscriberId}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    public SseEmitter streamData(@RequestHeader(name = "Last-Event-ID", required = false) String lastId,
                                 @RequestHeader(value = "User-Agent") String userAgent,
                                 @RequestHeader(value="X-TenantID",required = true) String tenantId,
                                 @PathVariable(value="topicName") String topicName,
                                 @PathVariable(value="subscriberId") String subscriberId,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        String ipAddress = request.getRemoteAddr();
        response.setHeader("Cache-Control", "no-store");
        return sseService.provisionUserSSE(tenantId,topicName,subscriberId,ipAddress,userAgent);
    }

    @PostMapping
    @CrossOrigin
    @PreAuthorize("hasRole('datastream-producer')")
    public ResponseEntity postData(@RequestHeader(value="X-TenantID",required = true) String tenantId,
                                   @RequestBody PayLoad data) {
    	sseService.sendMsg(tenantId,data);
        return new ResponseEntity(HttpStatus.OK);
    }

    @GetMapping(path = "/logout/{topicName}/{subscriberId}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    @Secured("datastream-consumer")
    public void logout(@RequestHeader(value="X-TenantID") String tenantId,
                                 @PathVariable(value="topicName") String topicName,
                                 @PathVariable(value="subscriberId") String subscriberId) {
        sseService.logOutUser(tenantId,topicName,subscriberId);
    }


    @Secured("datastream-admin")
    @GetMapping(path = "/getAllConnectionStats")
    public List<String> getAllConnectionStats() {
        return sseService.getAllConnectionStats();
    }



}