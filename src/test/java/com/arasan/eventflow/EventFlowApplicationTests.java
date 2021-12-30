package com.arasan.eventflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "oauth.issuer.uri = http://localhost:8080/auth/realms/phonebridge-cti/",
		"oauth.jwkset.uri = http://localhost:8080/auth/realms/phonebridge-cti/"
    }
)
class EventFlowApplicationTests {

	@Test
	void contextLoads() {
	}

}
