package com.server.consumer.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
// 不用扫描 provider，provider 是被代理的, 但是 microcommon 要扫描, 后期优化
@ComponentScan(basePackages = {"com.server", "com.seaflower.microcommon"})
public class ConsumerControllerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConsumerControllerApplication.class, args);
	}

}
