package com.server.provider.controller.hello;

import com.server.provider.api.hello.HelloInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/provider")
public class HelloController {

	@Autowired
	private HelloInterface helloInterface;

	@GetMapping("/sayHello")
	public String sayHello(@RequestParam("name") String name) {
		return helloInterface.hello(name);
	}
}
