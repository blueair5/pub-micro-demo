package com.server.provider.service.hello;

import com.server.provider.api.hello.HelloInterface;
import org.springframework.stereotype.Service;

@Service
public class HelloService implements HelloInterface {
	@Override
	public String hello(String name) {
		return "你好: " + name + "!";
	}
}
