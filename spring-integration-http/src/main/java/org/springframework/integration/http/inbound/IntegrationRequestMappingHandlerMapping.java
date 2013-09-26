/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.http.inbound;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The {@link org.springframework.web.servlet.HandlerMapping} implementation that
 * detects and registers {@link RequestMappingInfo}s for {@link HttpRequestHandlingEndpointSupport}
 * from a Spring Integration HTTP configuration of
 * {@code <inbound-channel-adapter/>} and {@code <inbound-gateway/>} elements.
 * <p/>
 * This class is automatically configured as bean in the application context on the parsing phase of
 * the {@code <inbound-channel-adapter/>} and {@code <inbound-gateway/>} elements, if there is none registered, yet.
 * However it can be configured as a regular bean with appropriate configuration for
 * {@link RequestMappingHandlerMapping}. It is recommended to have only one similar bean in the application context
 * and with the 'id' {@link org.springframework.integration.http.support.HttpContextUtils#HANDLER_MAPPING_BEAN_NAME}.
 * <p/>
 * In most cases Spring MVC offers to configure Request Mapping via {@link org.springframework.stereotype.Controller}
 * and {@link org.springframework.web.bind.annotation.RequestMapping},
 * and that's why Spring MVC Handler Mapping infrastructure relies on {@link org.springframework.web.method.HandlerMethod},
 * because different methods at the same {@link org.springframework.stereotype.Controller} user-class may have its own
 * {@link org.springframework.web.bind.annotation.RequestMapping}. From other side all Spring Integration HTTP Inbound
 * Endpoints are configured on the basis of the same {@link HttpRequestHandlingEndpointSupport} class and there is no
 * any {@link RequestMappingInfo} configuration without {@link org.springframework.web.method.HandlerMethod} in the Spring MVC.
 * Accordingly {@link IntegrationRequestMappingHandlerMapping} is some {@link org.springframework.web.servlet.HandlerMapping}
 * compromise implementation between method-level annotation and component-level (e.g. Spring Integration XML) configurations.
 *
 * @author Artem Bilan
 * @see RequestMapping
 * @see RequestMappingHandlerMapping
 * @since 3.0
 */
public final class IntegrationRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

	private static final Method HANDLE_REQUEST_METHOD = ReflectionUtils.findMethod(HttpRequestHandler.class,
			"handleRequest", HttpServletRequest.class, HttpServletResponse.class);

	private static final Method CREATE_REQUEST_MAPPING_INFO_METHOD;

	static {
		/**
		 * Need for full reuse {@link RequestMappingHandlerMapping}'s logic
		 * and makes this class Spring MVC version independent.
		 */
		CREATE_REQUEST_MAPPING_INFO_METHOD = ReflectionUtils.findMethod(RequestMappingHandlerMapping.class,
				"createRequestMappingInfo", org.springframework.web.bind.annotation.RequestMapping.class, RequestCondition.class);
		ReflectionUtils.makeAccessible(CREATE_REQUEST_MAPPING_INFO_METHOD);
	}

	@Override
	protected final boolean isHandler(Class<?> beanType) {
		return HttpRequestHandlingEndpointSupport.class.isAssignableFrom(beanType);
	}

	@Override
	protected final HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			handler = handlerMethod.getBean();
		}
		return super.getHandlerExecutionChain(handler, request);
	}

	@Override
	protected final void detectHandlerMethods(Object handler) {
		if (handler instanceof String) {
			handler = this.getApplicationContext().getBean((String) handler);
		}
		RequestMappingInfo mapping = this.getMappingForEndpoint((HttpRequestHandlingEndpointSupport) handler);
		this.registerHandlerMethod(handler, HANDLE_REQUEST_METHOD, mapping);
	}

	/**
	 * Created a {@link RequestMappingInfo} from a 'Spring Integration HTTP Inbound Endpoint' {@link RequestMapping}.
	 *
	 * @see RequestMappingHandlerMapping#getMappingForMethod
	 */
	private RequestMappingInfo getMappingForEndpoint(HttpRequestHandlingEndpointSupport endpoint) {
		final RequestMapping requestMapping = endpoint.getRequestMapping();

		org.springframework.web.bind.annotation.RequestMapping requestMappingAnnotation =
				new org.springframework.web.bind.annotation.RequestMapping() {
					public String[] value() {
						return requestMapping.getPathPatterns();
					}

					public RequestMethod[] method() {
						return requestMapping.getRequestMethods();
					}

					public String[] params() {
						return requestMapping.getParams();
					}

					public String[] headers() {
						return requestMapping.getHeaders();
					}

					public String[] consumes() {
						return requestMapping.getConsumes();
					}

					public String[] produces() {
						return requestMapping.getProduces();
					}

					public Class<? extends Annotation> annotationType() {
						return org.springframework.web.bind.annotation.RequestMapping.class;
					}
				};

		Object[] createRequestMappingInfoParams = new Object[]{requestMappingAnnotation, this.getCustomTypeCondition(endpoint.getClass())};
		return (RequestMappingInfo) ReflectionUtils.invokeMethod(CREATE_REQUEST_MAPPING_INFO_METHOD, this, createRequestMappingInfoParams);
	}

}
