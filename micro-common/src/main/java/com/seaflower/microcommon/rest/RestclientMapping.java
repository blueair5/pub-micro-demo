package com.seaflower.microcommon.rest;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestclientMapping {
	String name() default "";

	@AliasFor("path")
	String value() default "";

	@AliasFor("value")
	String path() default "";

	RequestMethod method() default RequestMethod.GET;

	String[] params() default {};

	String[] headers() default {};

	String[] consumes() default {};

	String[] produces() default {};

}
