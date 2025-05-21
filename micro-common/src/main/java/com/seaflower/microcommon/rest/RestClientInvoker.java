package com.seaflower.microcommon.rest;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.util.Tuple;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;

/**
 * 代理对象的构造类
 */
public class RestClientInvoker implements InvocationHandler {
	private RestTemplate _restTemplate;
	private String _microServiceName;
	private Class _contract;
	private String _contextPath;

	public RestClientInvoker(String microName, String contextPath, Class c, RestTemplate restTemplate) {
		_microServiceName = microName;
		_contextPath = contextPath;
		_contract = c;
		_restTemplate = restTemplate;
	}

	/**
	 * <p>
	 * consumerIntf 告诉 Proxy.newProxyInstance 的代理对象需要实现哪些接口,
	 * Proxy.newProxyInstance 会在运行时动态地生成一个全新的类（通常名字类似 $Proxy0, $Proxy1），
	 * 这个动态生成的类会自动实现 consumerIntf 接口中定义的所有方法。这个动态生成的类就是我们所说的代理类。
	 * <p/>
	 *
	 * <p>
	 * InvocationHandler 的核心:<br/>
	 * Proxy.newProxyInstance 的第三个参数是一个 InvocationHandler 接口的实例，
	 * 也就是你的 invoker 对象（new RestClientInvoker(...)）。
	 * <br/>
	 * <br/>
	 *
	 * Jdk 动态代理机制的核心规定是：当这个代理对象的任何方法被调用时，它都不会执行方法接口本身的实现，
	 * 而是统一将这个方法转发给 InvocationHandler 的 invoke 方法来处理。
	 * <br/>
	 * <br/>
	 *
	 * InvocationHandler 接口只有一个方法，签名如下: <br/>
	 * <code>
	 * public Object invoke(Object proxy, Method method, Object[] args) throws Throwable;
	 * </code>
	 * </p>
	 *
	 * @param microName 微服务名称
	 * @param contextPath 接口路径
	 * @param restTemplate RestTemplate
	 * @param consumerIntf 代理的类
	 * @return 被代理的对象
	 * @param <T>
	 */
	public static <T> T createProxy(String microName, String contextPath, RestTemplate restTemplate,
									Class<?> consumerIntf) {
		RestClientInvoker invoker = new RestClientInvoker(microName, contextPath, consumerIntf, restTemplate);
		return (T) Proxy.newProxyInstance(RestClientInvoker.class.getClassLoader(), new Class<?>[] { consumerIntf },
			invoker);
	}

	/**
	 *
	 * @param microNames
	 * @param contextPath port/pty/
	 * @param restTemplate
	 * @param consumerIntf 对应的类
	 * @return
	 * @param <T> T 的推断，T createProxy 的返回值是一个 T, 根据 createProxy 的返回值推断 T
	 */
	public static <T> List<T> createListProxys(List<String> microNames, String contextPath, RestTemplate restTemplate,
											   Class<?> consumerIntf) {
		List<T> proxies = new ArrayList<T>(microNames.size());
		for (String micName : microNames) {
			proxies.add(createProxy(micName, contextPath, restTemplate, consumerIntf));
		}
		return proxies;
	}

	/**
	 * Map 类型的，这种情况，Map 的这个 key 就是微服务的名称
	 * @param microNames
	 * @param contextPath
	 * @param restTemplate
	 * @param consumerIntf
	 * @return
	 * @param <T>
	 */
	public static <T> Map<String, T> createMapProxys(List<String> microNames, String contextPath,
													 RestTemplate restTemplate, Class<?> consumerIntf) {
		Map<String, T> proxies = new HashMap<String, T>(microNames.size());
		for (String micName : microNames) {
			proxies.put(micName, createProxy(micName, contextPath, restTemplate, consumerIntf));
		}
		return proxies;
	}

	/**
	 * <p>
	 * 被代理类的所有方法都会被转到这个方法上，我们要在这里处理 RestClientMapping 的注解标识的方法
	 * </p>
	 *
	 * <p>
	 * 只有被代理类的方法实际被执行的时候，才会被转到这个 invoke 方法上
	 * </p>
	 *
	 * @return
	 * @throws Throwable
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			RestclientMapping mapping = method.getAnnotation(RestclientMapping.class);

			// mapping 可能长这样：
			//  @RestclientMapping(path = "/api/pex/expense/delBill",method = RequestMethod.GET)
			if (Objects.isNull(mapping)) {
				// 如果 mapping 是空的，我们直接抛出异常。
				// 因为进入到这个 invoke，说明服务发起了实际的方法调用, 那么这个实际的方法上，一定要加 RestClientMapping
				// 来标识我们要发起 http 调用的远端地址
				throw new IllegalStateException(getMethodFullPath(method) + " 没有配置RestclientMapping");
			}

			// 处理请求的 URL, 先处理  RequestParam 的参数
			String requestPath = pupulateGetParamter(resolveMethodPath(method), method, args);
			// 处理 PathVariable 的变量
			requestPath = populatePathVar(requestPath, method, args);

			// 判断是不是合法的 URL
			if (!requestPath.startsWith("/")) {
				System.out.println("path error" + requestPath + "必须以 '/' 开头");
				throw new IllegalArgumentException(getMethodFullPath(method) + "'s request path is illegal");
			}

			Object body = getPostBody(method, args);

			// 准备发起 http 调用
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON_UTF8));
			headers.setContentType(MediaType.APPLICATION_JSON);

			ResponseEntity responseEntity = null;
			// 这里我们简化, 将微服务实例当成 requestPath 的一部分, microService 当成类上的 RequestMapping
			// </微服务实例/具体的请求地址>
			requestPath = "/" + _microServiceName + requestPath;

			// 拼接上端口号和上下文路径
//			requestPath += (_contextPath.startsWith("/") ? "" : "/") + _contextPath;
//			requestPath = (_contextPath.startsWith("/") ? "" : "/") + _contextPath + requestPath;
			String prefix = "http://localhost:8089";
			// 拼接出来应该是 prefix/serverContextPath/具体的路径
			requestPath = prefix + requestPath;

			RequestEntity requestEntity = new RequestEntity(body, headers,
				HttpMethod.valueOf(mapping.method().name()), URI.create(requestPath));
			// 发起调用，兼容泛型和非泛型的情况
			responseEntity = _restTemplate.exchange(requestEntity,
				ParameterizedTypeReference.forType(method.getGenericReturnType()));

			// 判断状态，然后返回结果
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				throw new RuntimeException(resolveMethodPath(method) + " request is error");
			}
			return responseEntity.getBody();
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	/**
	 *
	 * @param methodPath 从 RestClientMapping 中解析出的 path 地址
	 * @param method 调用的方法
	 * @param args 方法的参数
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String pupulateGetParamter(String methodPath, Method method, Object[] args) throws UnsupportedEncodingException {
		RestclientMapping mapping = method.getAnnotation(RestclientMapping.class);
		Parameter[] params = method.getParameters();
		Parameter param = null;
		String paramStr = ""; // 最终拼接出来的 URL 内容

		// 处理方法的参数, 提供出来的方法应该指定 @RequestParam 这样的内容
		// 例如：
		// @RestclientMapping(path = "/api/pex/expense/delBill",method = RequestMethod.GET)
		// void delBill(@RequestParam("billType") String billType,@RequestParam("billId") String billId, boolean... b);
		for (int i = 0; i < params.length; i ++) {
			Object paramValue = args[i];
			if (paramValue == null || paramValue.toString().isEmpty()) {
				continue;
			}
			param = params[i];
			// 排除掉 RequestBody 和基本类型，String 类型的参数

			// 实际上我们只处理 @RequestParam 注解的参数,
			// 还有就是将一个对象搞成一个 Map，将这个 Map 拼接成一个 Get 中的参数

			// 尝试从参数上获取 RequestParam 注解
			RequestParam requestParam = param.getAnnotation(RequestParam.class);
			if (Objects.nonNull(requestParam)) {
				// 获取 RequestParam 中的名称
				String urlParamName = paramName(requestParam);
				// 没有直接抛出异常，所以这个一定要配
				if (urlParamName == null || urlParamName.isEmpty()) {
					throw new IllegalArgumentException(method.getName() + " RequestParam的name属性为空");
				}
				// 拼接出 Get 请求
				paramStr += urlParamName + "=" + URLEncoder.encode(paramValue.toString(), "utf-8") + "&";
			} else {
				Map mapValue = null; // 这个具体的实现不写了，就是将这个转换成一个 Map
				for (Object key : mapValue.keySet()) {
					Object v = mapValue.get(key);
					if (v != null) {
						paramStr += key + "=" + URLEncoder.encode(mapValue.get(key).toString(), "utf-8") + "&";
					}
				}
			}
		}

		if (paramStr.length() > 0) {
			methodPath += "?" + paramStr.substring(0, paramStr.length() - 1);
		}
		return methodPath;
	}

	/**
	 * 获取 RestClientMapping 中的 path 地址
	 * @param method
	 * @return
	 */
	private String resolveMethodPath(Method method) {
		String methodPath = "";
		// 获取这个类上的注解，目前没有想到什么场景，RestClientMapping 注解类，可以为当前类的所有的方法都加一个链接前缀
		RestclientMapping mapping = (RestclientMapping) _contract.getAnnotation(RestclientMapping.class);
		if (mapping != null) {
			methodPath = mappingPath(mapping);
		}

		// 获取方法上的注解中的 path
		mapping = method.getAnnotation(RestclientMapping.class);
		if (mapping != null) {
			// 类上的 URL 前缀 + 方法上的 URL 前缀
			methodPath += mappingPath(mapping);
		}

		return methodPath;
	}

		/**
		 * 获取方法的全限定名
		 * @param method
		 * @return
		 */
	private String getMethodFullPath(Method method) {
		return method.getDeclaringClass().getCanonicalName() + "." + method.getName();
	}

	private String mappingPath(RestclientMapping mapping) {
		if (mapping.path() != null && !mapping.path().isEmpty()) {
			return mapping.path();
		}
		return mapping.value();
	}

	/**
	 * 从 RequestParam 中获取 param 名称
	 * @param param
	 * @return
	 */
	private String paramName(RequestParam param) {
		if (param.name() != null && !param.name().isEmpty()) {
			return param.name();
		}
		return param.value();
	}

	private String populatePathVar(String methodPath, Method method, Object[] args) {
		Parameter[] params = method.getParameters();
		Parameter param = null;
		for (int i = 0; i < params.length; i++) {
			Object pathVarValue = args[i];
			if (pathVarValue == null || pathVarValue.toString().isEmpty()) {
				continue;
			}
			param = params[i];
			PathVariable pathVar = param.getAnnotation(PathVariable.class);
			if (pathVar != null) {
				String pathVarName = pathVarName(pathVar);
				methodPath = methodPath.replace("{" + pathVarName + "}", pathVarValue.toString());
			}
		}
		return methodPath;
	}

	private String pathVarName(PathVariable pathVar) {
		if (pathVar.name() != null && !pathVar.name().isEmpty()) {
			return pathVar.name();
		}
		return pathVar.value();
	}

	/**
	 * 获取发送的实体对象, 处理 post 请求, 找第一个 RequestBody
	 * @param method
	 * @param args
	 * @return
	 */
	private Object getPostBody(Method method, Object[] args) {
		RestclientMapping mapping = method.getAnnotation(RestclientMapping.class);
		if (!mappingMethod(mapping).contains(RequestMethod.POST)) {
			return null;
		}

		Parameter[] params = method.getParameters();
		Parameter param = null;

		for (int i = 0; i < params.length; i ++) {
			param = params[i];
			RequestBody requestBody = param.getAnnotation(RequestBody.class);
			if (Objects.isNull(requestBody) || Objects.isNull(args[i])) {
				continue;
			}
			// 找到第一个 RequestBody 返回
			return args[i];
		}
		return null;
	}

	private List<RequestMethod> mappingMethod(RestclientMapping mapping) {
		return Arrays.asList(mapping.method());
	}

	private Tuple generalMicroServiceInfo(String microName, String path) {
		String[] split = microName.split("-");
		String m = split[0];
		String p = path;
		if (split.length>1) {
			p = "/" + split[1] + path;
		}
		return new Tuple(m, p);
	}
}




