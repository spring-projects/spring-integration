/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.http.inbound;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.condition.NameValueExpression;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The {@link org.springframework.web.servlet.HandlerMapping} implementation that
 * detects and registers {@link RequestMappingInfo}s for
 * {@link HttpRequestHandlingEndpointSupport} from a Spring Integration HTTP configuration
 * of {@code <inbound-channel-adapter/>} and {@code <inbound-gateway/>} elements.
 * <p>
 * This class is automatically configured as a bean in the application context during the
 * parsing phase of the {@code <inbound-channel-adapter/>} and {@code <inbound-gateway/>}
 * elements, if there is none registered, yet. However it can be configured as a regular
 * bean with appropriate configuration for {@link RequestMappingHandlerMapping}.
 * It is recommended to have only one similar bean in the application context using the 'id'
 * {@link org.springframework.integration.http.config.HttpContextUtils#HANDLER_MAPPING_BEAN_NAME}.
 * <p>
 * In most cases, Spring MVC offers to configure Request Mapping via
 * {@code org.springframework.stereotype.Controller} and
 * {@link org.springframework.web.bind.annotation.RequestMapping}.
 * That's why Spring MVC's Handler Mapping infrastructure relies on
 * {@link org.springframework.web.method.HandlerMethod}, as different methods at the same
 * {@code org.springframework.stereotype.Controller} user-class may have their own
 * {@link org.springframework.web.bind.annotation.RequestMapping}.
 * On the other side, all Spring Integration HTTP Inbound Endpoints are configured on
 * the basis of the same {@link BaseHttpInboundEndpoint} class and there is no
 * single {@link RequestMappingInfo} configuration without
 * {@link org.springframework.web.method.HandlerMethod} in Spring MVC.
 * Accordingly {@link IntegrationRequestMappingHandlerMapping} is a
 * {@link org.springframework.web.servlet.HandlerMapping}
 * compromise implementation between method-level annotations and component-level
 * (e.g. Spring Integration XML) configurations.
 * <p>
 * Starting with version 5.1, this class implements {@link DestructionAwareBeanPostProcessor} to
 * register HTTP endpoints at runtime for dynamically declared beans, e.g. via
 * {@link org.springframework.integration.dsl.context.IntegrationFlowContext}, and unregister
 * them during the {@link BaseHttpInboundEndpoint} destruction.
 *<p>
 * This class extends the Spring MVC {@link RequestMappingHandlerMapping} class, inheriting
 * most of its logic, especially {@link #handleNoMatch(java.util.Set, String, HttpServletRequest)},
 * which throws a specific {@code 4xx} error for the HTTP response, when mapping doesn't match
 * for some reason, preventing calls to any remaining mapping handlers in the application context.
 * For this reason, configuring the same path for both Spring Integration and
 * Spring MVC request mappings (e.g. `POST` in one and `GET` in the other) is not supported;
 * the MVC mapping will not be found.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 *
 * @see RequestMapping
 * @see RequestMappingHandlerMapping
 */
public final class IntegrationRequestMappingHandlerMapping extends RequestMappingHandlerMapping
		implements ApplicationListener<ContextRefreshedEvent>, DestructionAwareBeanPostProcessor {

	private static final Method HANDLE_REQUEST_METHOD =
			ReflectionUtils.findMethod(HttpRequestHandler.class, "handleRequest", HttpServletRequest.class,
					HttpServletResponse.class);

	private final AtomicBoolean initialized = new AtomicBoolean();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (this.initialized.get() && isHandler(bean.getClass())) {
			detectHandlerMethods(bean);
		}

		return bean;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (isHandler(bean.getClass())) {
			RequestMappingInfo mapping = getMappingForEndpoint((BaseHttpInboundEndpoint) bean);
			if (mapping != null) {
				unregisterMapping(mapping);
			}
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return isHandler(bean.getClass());
	}

	@Override
	protected boolean isHandler(Class<?> beanType) {
		return HttpRequestHandlingEndpointSupport.class.isAssignableFrom(beanType);
	}

	@Override
	protected HandlerExecutionChain getHandlerExecutionChain(Object handlerArg, HttpServletRequest request) {
		Object handler = handlerArg;
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			Object bean = handlerMethod.getBean();
			if (bean instanceof HttpRequestHandlingEndpointSupport) {
				handler = bean;
			}
		}

		return super.getHandlerExecutionChain(handler, request);
	}

	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		if (handler instanceof HandlerMethod) {
			return super.getCorsConfiguration(handler, request);
		}
		else {
			return super.getCorsConfiguration(new HandlerMethod(handler, HANDLE_REQUEST_METHOD), request);
		}
	}

	@Override
	protected void detectHandlerMethods(Object handlerArg) {
		Object handler = handlerArg;
		if (handler instanceof String) {
			ApplicationContext applicationContext = getApplicationContext();
			if (applicationContext != null) {
				handler = applicationContext.getBean((String) handler);
			}
			else {
				throw new IllegalStateException("No application context available to lookup bean '"
						+ handler + "'");
			}
		}
		RequestMappingInfo mapping = getMappingForEndpoint((BaseHttpInboundEndpoint) handler);
		if (mapping != null) {
			registerMapping(mapping, handler, HANDLE_REQUEST_METHOD);
		}
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
		CrossOrigin crossOrigin = ((BaseHttpInboundEndpoint) handler).getCrossOrigin();
		if (crossOrigin != null) {
			return buildCorsConfiguration(crossOrigin, mappingInfo);
		}
		return null;
	}

	private static CorsConfiguration buildCorsConfiguration(CrossOrigin crossOrigin, RequestMappingInfo mappingInfo) {
		CorsConfiguration config = new CorsConfiguration();
		for (RequestMethod requestMethod : crossOrigin.getMethod()) {
			config.addAllowedMethod(requestMethod.name());
		}
		config.setAllowedHeaders(Arrays.asList(crossOrigin.getAllowedHeaders()));
		config.setExposedHeaders(Arrays.asList(crossOrigin.getExposedHeaders()));
		Boolean allowCredentials = crossOrigin.getAllowCredentials();
		config.setAllowCredentials(allowCredentials);
		List<String> allowedOrigins = Arrays.asList(crossOrigin.getOrigin());
		if (Boolean.TRUE.equals(allowCredentials)
				&& CollectionUtils.contains(allowedOrigins.iterator(), CorsConfiguration.ALL)) {
			config.setAllowedOriginPatterns(allowedOrigins);
		}
		else {
			config.setAllowedOrigins(allowedOrigins);
		}

		if (crossOrigin.getMaxAge() != -1) {
			config.setMaxAge(crossOrigin.getMaxAge());
		}
		if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
			for (RequestMethod allowedMethod : mappingInfo.getMethodsCondition().getMethods()) {
				config.addAllowedMethod(allowedMethod.name());
			}
		}
		if (CollectionUtils.isEmpty(config.getAllowedHeaders())) {
			for (NameValueExpression<String> headerExpression :
					mappingInfo.getHeadersCondition().getExpressions()) {

				if (!headerExpression.isNegated()) {
					config.addAllowedHeader(headerExpression.getName());
				}
			}
		}
		return config.applyPermitDefaultValues();
	}

	/**
	 * Created a {@link RequestMappingInfo} from a
	 * 'Spring Integration HTTP Inbound Endpoint' {@link RequestMapping}.
	 * @see RequestMappingHandlerMapping#getMappingForMethod
	 */
	@Nullable
	private RequestMappingInfo getMappingForEndpoint(BaseHttpInboundEndpoint endpoint) {
		final RequestMapping requestMapping = endpoint.getRequestMapping();

		if (ObjectUtils.isEmpty(requestMapping.getPathPatterns())) {
			return null;
		}

		Map<String, Object> requestMappingAttributes = new HashMap<>();
		requestMappingAttributes.put("name", endpoint.getComponentName());
		requestMappingAttributes.put("value", requestMapping.getPathPatterns());
		requestMappingAttributes.put("path", requestMapping.getPathPatterns());
		requestMappingAttributes.put("method", requestMapping.getRequestMethods());
		requestMappingAttributes.put("params", requestMapping.getParams());
		requestMappingAttributes.put("headers", requestMapping.getHeaders());
		requestMappingAttributes.put("consumes", requestMapping.getConsumes());
		requestMappingAttributes.put("produces", requestMapping.getProduces());

		org.springframework.web.bind.annotation.RequestMapping requestMappingAnnotation =
				AnnotationUtils.synthesizeAnnotation(requestMappingAttributes,
						org.springframework.web.bind.annotation.RequestMapping.class, null);
		return createRequestMappingInfo(requestMappingAnnotation, getCustomTypeCondition(endpoint.getClass()));
	}

	/**
	 * {@link HttpRequestHandlingEndpointSupport}s may depend on auto-created
	 * {@code requestChannel}s, so MVC Handlers detection should be postponed
	 * as late as possible.
	 * @see RequestMappingHandlerMapping#afterPropertiesSet()
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext().equals(getApplicationContext()) && !this.initialized.getAndSet(true)) {
			super.afterPropertiesSet();
		}
	}

	@Override
	public void afterPropertiesSet() {
		// No-op in favor of onApplicationEvent
	}

}
