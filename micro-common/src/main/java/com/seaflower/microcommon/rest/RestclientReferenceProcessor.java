package com.seaflower.microcommon.rest;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 这个需要在 spring 容器中
 */
@Component
public class RestclientReferenceProcessor implements BeanPostProcessor, Ordered {
	private String serverContextPath;

	// 服务的前缀
	private final String SERVER_CONTEXT_PATH = "server.servlet.context-path";

	// 目标服务的端口
	private final String SERVER_PORT = "server.port";

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private RestTemplate _restTemplate;


	/**
	 * 将地址的前缀拼接出来
	 */
	@PostConstruct
	private void init() {
		String port = applicationContext.getEnvironment().getProperty(SERVER_PORT);
		if (StringUtils.isEmpty(port)) {
			port = "";
		}
		String contextPath = applicationContext.getEnvironment().getProperty(SERVER_CONTEXT_PATH);
		if (StringUtils.isEmpty(contextPath)) {
			contextPath = "";
		}
		serverContextPath = "/" +contextPath;
	}

	// 在 Bean 初始化之前进行一些操作
	// 如果想要下面的方法打断点，应该在这个方法上打断点，然后再启动项目
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		// 对给定的 Bean 的字段，执行指定的回调
		ReflectionUtils.doWithFields(bean.getClass(), field -> {
			processRestClientField(bean, field);
		});

		return bean;
	}

	// 之后不用处理
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	// 判断 Bean 中的每一个 field，处理带有 @RestClientReference 注解的字段
	private void processRestClientField(Object bean, Field field) {
		try {
			RestClientReference reference = field.getAnnotation(RestClientReference.class);
			if (Objects.isNull(reference)) {
				return;
			}

			// 设置可以访问 private 的属性
			ReflectionUtils.makeAccessible(field);

			// 判断对应的 field 是不是空的, 如果不是空的，说明就是已经初始化过了，不再进行初始化
			if (Objects.nonNull(ReflectionUtils.getField(field, bean))) {
				return;
			}

			Class consumerInterface = reference.consumerInterface();
			// 没有指定 consumerInterface 的话，我们直接从 field 中获取我们要代理的类的类型 <class>
			if (consumerInterface.equals(Void.class)) {
				consumerInterface = field.getType(); // 这个就是 Bean 的类型, 比如 PexService(接口)
				if (List.class.isAssignableFrom(consumerInterface)) {
					/*
					 * 如果这个字段是 List, 就是 List<PexService> 这样的形式，我们要将 PexService 取出来
					 */
					ParameterizedType listGenericType = (ParameterizedType) field.getGenericType();
					consumerInterface = Class.forName(listGenericType.getActualTypeArguments()[0].getTypeName());
				} else if (Map.class.isAssignableFrom(consumerInterface)) {
					/*
					 * 下面的这种形式的
					 * @Autowired(required = false)
					 * private Map<String, ISettlementCheckService> settlementCheckService;
					 */
					ParameterizedType mapGenericType = (ParameterizedType) field.getGenericType();
					consumerInterface = Class.forName(mapGenericType.getActualTypeArguments()[1].getTypeName());
				}
			}

			// 获取微服务名称, 从注解中获取
			String[] microServiceNames = reference.microServiceNames();
			// 注解注释的，必须指明微服务名称
			if (microServiceNames == null || microServiceNames.length == 0) {
				throw new IllegalStateException(
					field.getName() + "'s RestClientReference anno's microServiceNames is empty");
			}

			// 针对不同类型的 field 类型的, 我们设置不同的值
			if (List.class.isAssignableFrom(field.getType())) {
				ReflectionUtils.setField(field, bean, RestClientInvoker.createListProxys(
					Arrays.asList(microServiceNames), serverContextPath, _restTemplate, consumerInterface));
			} else if (Map.class.isAssignableFrom(field.getType())) {
				ReflectionUtils.setField(field, bean, RestClientInvoker.createMapProxys(
					Arrays.asList(microServiceNames), serverContextPath, _restTemplate, consumerInterface));
			} else {
				ReflectionUtils.setField(field, bean, RestClientInvoker.createProxy(microServiceNames[0],
					serverContextPath, _restTemplate, consumerInterface));
			}

		} catch (Exception e) {
			throw new RuntimeException();
		}
	}



		/**
		 * 指定 Spring 容器中 Bean 的执行顺序。LOWEST_PRECEDENCE 是最低优先级
		 * @return
		 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 1;
	}
}
