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
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class PerfTest {

    Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    String oauthTokenUrl = "http://34.134.149.85:8080/auth/realms/phonebridge-cti/protocol/openid-connect/token";
    //public String baseUrl = "http://104.154.188.48:8081/stream";
    public String baseUrl = "http://localhost:8081/stream";
    public String topicName = "cti";
    public String accountId = "eupraxia";
    public int noOfConsumer = 100;
    public int notOfEventsPerConsumer = 60;
    public int threadPoolSize = 100;
    AtomicInteger subscriberInt;
    WebClient client = WebClient.create(baseUrl);

    PerfTest(){
        subscriberInt = new AtomicInteger(0);
    }

    private void startConsumer() throws JsonProcessingException, InterruptedException {
        logger.info("Starting consumers");
        String oauthToken = getOauthToken();
        ExecutorService executors=null;
        try {
            executors = Executors.newFixedThreadPool(threadPoolSize);
            List<Callable<Boolean>> lst = new ArrayList<>(noOfConsumer);
            for (int i = 0; i < noOfConsumer; i++) {
                lst.add(() -> {
                    String subscriberId = getNewSubscriberId();
                    Flux<PayLoad> payLoads = null;
                    try {
                        payLoads = consumeServerSentEvent(subscriberId, oauthToken);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    StepVerifier.Step<PayLoad> step = StepVerifier.create(payLoads);
                    verifyEachStep(step, subscriberId, notOfEventsPerConsumer);
                    return true;
                });
            }
            executors.invokeAll(lst);
        }finally {
            logger.info("Shutting down consumer executor!");
            executors.shutdown();
        }
    }

    private void verifyEachStep(StepVerifier.Step<PayLoad> step,String subscriberId,int noOfItems) {
        int counter=0;
        while(counter<noOfItems) {
            final int finalCounter = counter;
            StepVerifier.Step st = step
                    .thenAwait(Duration.ofSeconds(3))
                    .expectNextMatches(pl -> {
                assertThat(pl.getDestSubscriberId()).isEqualTo(subscriberId);
                //assertThat(pl.getMessage()).isEqualTo(String.valueOf(finalCounter));
                return true;
            });
            counter++;
            if(counter==noOfItems){
                st.thenCancel().verify();
                logger.info("--->>>"+subscriberId+",finishing consumer!");
            }else{
                logger.info("--->>>"+subscriberId+",waiting for "+(noOfItems-counter)+"more items!");
            }
        }
    }

    private Flux<PayLoad> consumeServerSentEvent(String subscriberId, String oauthToken) throws JsonProcessingException {
        Flux<PayLoad> payLoads = client
                .get()
                .uri("/{accountId}/{topicName}/{subscriberId}",accountId,topicName,subscriberId)
                .header("Authorization","Bearer "+oauthToken)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(PayLoad.class);
        return payLoads;
        }

    //Get OAUTH token
    public String getOauthToken() throws JsonProcessingException {
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

    private void startProducer() throws JsonProcessingException, InterruptedException {
        Thread.sleep(noOfConsumer*250);
        logger.info("Starting producers");
        String oauthToken = getOauthToken();
        ExecutorService executor = null;
        try {
            executor = Executors.newFixedThreadPool(threadPoolSize);
            List<Callable<Boolean>> lst = new ArrayList<>(noOfConsumer);
            for (int i = 0; i < noOfConsumer; i++) {
                String subscriberId = "subscriber" + (i + 1);
                lst.add(() -> postServerSentEvents(subscriberId, oauthToken));
            }
            executor.invokeAll(lst);
        }finally {
            logger.info("Shutting down producer executor!");
            executor.shutdown();
        }
    }

    private boolean postServerSentEvents(String subscriberId,String oauthToken) throws InterruptedException {
        for(int i=0;i<notOfEventsPerConsumer;i++){
            String message=String.valueOf(i);
            PayLoad pl = generateEvent(subscriberId,message);
            int retryCount=0;
            ResponseEntity<Void> result = null;
            while(retryCount<3 && result==null) {
                try {
                    result = client
                            .post()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(pl))
                            .header("Authorization", "Bearer " + oauthToken)
                            .retrieve()
                            .toBodilessEntity()
                            .onErrorStop().block();
                }catch(Exception ex){
                    logger.error("ERRORRRRR->",ex);
                }
                retryCount++;
                }
            logger.info("Posting-->"+subscriberId+",payload="+pl.toString()+",Response-->"+result.toString());
            Thread.sleep(1000);
        }
        return true;
    }

    private PayLoad generateEvent(String subscriberId,String message){
        PayLoad pl = new PayLoad();
        pl.setAccountId(accountId);
        pl.setTopicName(topicName);
        pl.setPayLoadId(message);
        pl.setSource("admin");
        pl.setDestSubscriberId(subscriberId);
        pl.setMessage(message);
        //pl.setMsgPostedOn(LocalDateTime.now());
        return pl;
    }

    //@Test
    public void consumerProducerTest() throws InterruptedException {
        ExecutorService executor = null;
        try{
            executor=Executors.newFixedThreadPool(2);
        List<Callable<Boolean>> lst = new ArrayList<>();
        lst.add(
                ()-> {
                    try {
                        startConsumer();
                        } catch (JsonProcessingException | InterruptedException e) {
                        e.printStackTrace();
                        }
                    return true;
                    });

        lst.add(
                ()-> {
                        try {
                            startProducer();
                        } catch (JsonProcessingException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        return true;
                });
        executor.invokeAll(lst);
    }finally {
            logger.info("Shutting down main executor!");
            executor.shutdown();
        }
        }
}
