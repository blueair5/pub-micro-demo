package com.server.provider.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.server.provider"})
public class ServerProviderControllerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerProviderControllerApplication.class, args);
	}

}
