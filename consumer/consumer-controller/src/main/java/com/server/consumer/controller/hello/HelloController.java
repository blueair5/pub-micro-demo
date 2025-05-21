package com.server.consumer.controller.hello;

import com.seaflower.microcommon.rest.RestClientReference;
import com.server.provider.api.hello.HelloInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/consumer")
public class HelloController {

	// 不扫描 micro-common 可能会报找不到 helloInterface
	@RestClientReference(microServiceNames = "provider")
	@Autowired(required = false)
	private HelloInterface helloInterface;

	@GetMapping("/sayHello")
	public String sayHello(@RequestParam("name") String name) {
		String msg = helloInterface.hello(name);
		return helloInterface.hello(name);
	}

}
