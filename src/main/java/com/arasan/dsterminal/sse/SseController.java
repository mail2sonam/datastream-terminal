package com.arasan.dsterminal.sse;

import com.arasan.dsterminal.dto.PayLoad;
import com.arasan.dsterminal.sse.terminalmgmt.ISSETerminalServ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stream")
public class SseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseController.class);

    ISSETerminalServ sseService;

    public SseController(ISSETerminalServ sseService){
        this.sseService=sseService;
    }

    @GetMapping(path = "/{tenantId}/{topicName}/{subscriberId}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    @PreAuthorize("hasRole('datastream-consumer')")
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
    public ResponseEntity<Map<String,String>> postData(@RequestHeader(value="X-TenantID",required = true) String tenantId,
                                                       @RequestBody PayLoad data) {
    	String correlationId=sseService.sendMsg(tenantId,data);
        Map<String,String> result=new HashMap<>();
        result.put("correlationId",correlationId);
        result.put("receivedOn", ZonedDateTime.now(ZoneOffset.systemDefault()).withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return ResponseEntity.ok(result);
    }

    @GetMapping(path = "/logout/{topicName}/{subscriberId}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    @PreAuthorize("hasRole('datastream-consumer')")
    public void logout(@RequestHeader(value="X-TenantID") String tenantId,
                                 @PathVariable(value="topicName") String topicName,
                                 @PathVariable(value="subscriberId") String subscriberId) {
        sseService.logOutUser(tenantId,topicName,subscriberId);
    }


    @GetMapping(path = "/getAllConnectionStats")
    @PreAuthorize("hasRole('datastream-admin')")
    public List<String> getAllConnectionStats() {
        return sseService.getAllConnectionStats();
    }



}