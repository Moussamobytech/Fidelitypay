package com.Api.Fidelitypay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FidelitypayApplication {

	public static void main(String[] args) {
		SpringApplication.run(FidelitypayApplication.class, args);
	}

}
