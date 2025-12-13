package com.Api.Fidelitypay;

import org.springframework.boot.SpringApplication;

public class TestFidelitypayApplication {

	public static void main(String[] args) {
		SpringApplication.from(FidelitypayApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
