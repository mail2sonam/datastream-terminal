package com.arasan.eventflow;

import com.arasan.dsterminal.dto.PayLoad;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PerfTest {

    Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    String oauthTokenUrl = "http://34.134.149.85:8080/auth/realms/phonebridge-cti/protocol/openid-connect/token";
    String baseUrl = "http://104.154.188.48:8081/stream";
    String topicName = "cti";
    String accountId = "eupraxia";

    AtomicInteger subscriberInt;

    PerfTest(){
        subscriberInt = new AtomicInteger(0);
    }

    @Test
    public void testConsumer() throws JsonProcessingException {
        this.consumeServerSentEvent(getNewSubscriberId());
    }

    private void consumeServerSentEvent(String subscriberId) throws JsonProcessingException {

        List<PayLoad> payLoads = WebTestClient
                .bindToServer()
                .responseTimeout(Duration.ofMinutes(1))
                .defaultHeader("Authorization","Bearer "+getOauthToken())
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri("/{accountId}/{topicName}/{subscriberId}",accountId,topicName,subscriberId)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(PayLoad.class)
                .getResponseBody()
                .take(10)
                .collectList()
                .block();

        payLoads.forEach(x -> {
                logger.info(x.toString());
                });
        }

    private void postServerSentEvent(){

    }

    //Get OAUTH token
    private String getOauthToken() throws JsonProcessingException {
        WebClient client = WebClient.builder().baseUrl(oauthTokenUrl)
                .build();
        ParameterizedTypeReference<ServerSentEvent<String>> type
                = new ParameterizedTypeReference<ServerSentEvent<String>>() {};
        String json = client.post()
                .header("Content-Type","application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData("grant_type","client_credentials")
                        .with("scope","openid")
                        .with("client_id","phonebridge-cti-client")
                        .with("client_secret","pV2TSCP3uU1E9COUDJ8d0FsKSmGCabiR"))
                .retrieve().bodyToMono(String.class)
                .block();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);
        String oauthToken = jsonNode.get("access_token").asText();
        logger.info("Oauth generated-->"+oauthToken);
        return oauthToken;
    }

    private String getNewSubscriberId(){
        return "subscriber"+subscriberInt.addAndGet(1);
    }


}
