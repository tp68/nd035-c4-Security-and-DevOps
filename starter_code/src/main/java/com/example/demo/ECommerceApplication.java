package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@EnableJpaRepositories("com.example.demo.model.persistence.repositories")
@EntityScan("com.example.demo.model.persistence")
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class ECommerceApplication {

	private static final Logger log = LoggerFactory.getLogger(ECommerceApplication.class);

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder(){
		return new BCryptPasswordEncoder();
	}

	public static void main(String[] args) {
		SpringApplication.run(ECommerceApplication.class, args);
		log.info("ECommerceApplication started successfully");
	}

}
