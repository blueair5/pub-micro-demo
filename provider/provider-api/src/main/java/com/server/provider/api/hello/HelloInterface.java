package com.server.provider.api.hello;

import com.seaflower.microcommon.rest.RestclientMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

public interface HelloInterface {
	// 这个和 controller 中的 GetMapping 是一样的, 类上的 @RequestMapping 我们当作微服务的 url
	@RestclientMapping(path = "/sayHello", method = RequestMethod.GET)
	String hello(@RequestParam("name") String name);
}
