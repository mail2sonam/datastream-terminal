package com.arasan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DataStreamTerminalApplication implements CommandLineRunner {

	Logger log = LoggerFactory.getLogger(DataStreamTerminalApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DataStreamTerminalApplication.class, args);
	}

	@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") private String issuerUri;
	@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") private String jwkSetUri;

	@Override
	public void run(String... args) throws Exception {
		log.info("0.0.5-SNAPSHOT --> IssuerURI="+issuerUri+", JWKSetUri="+jwkSetUri);
	}
}
