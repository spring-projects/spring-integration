/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.integration.webflux.inbound;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.http.config.HttpContextUtils;
import org.springframework.integration.http.inbound.BaseHttpInboundEndpoint;
import org.springframework.integration.http.inbound.CrossOrigin;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.result.condition.NameValueExpression;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;

/**
 * The {@link org.springframework.web.reactive.HandlerMapping} implementation that
 * detects and registers {@link org.springframework.web.reactive.result.method.RequestMappingInfo}s for
 * {@link org.springframework.integration.http.inbound.HttpRequestHandlingEndpointSupport}
 * from a Spring Integration HTTP configuration
 * of {@code <inbound-channel-adapter/>} and {@code <inbound-gateway/>} elements.
 * <p>
 * This class is automatically configured as a bean in the application context during the
 * parsing phase of the {@code <inbound-gateway/>}
 * elements, if there is none registered, yet. However it can be configured as a regular
 * bean with appropriate configuration for
 * {@link org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping}.
 * It is recommended to have only one similar bean in the application context using the 'id'
 * {@link org.springframework.integration.webflux.support.WebFluxContextUtils#HANDLER_MAPPING_BEAN_NAME}.
 * <p>
 * In most cases, Spring MVC offers to configure Request Mapping via
 * {@code org.springframework.stereotype.Controller} and
 * {@link org.springframework.integration.http.inbound.RequestMapping}.
 * That's why Spring MVC's Handler Mapping infrastructure relies on
 * {@link org.springframework.web.method.HandlerMethod}, as different methods at the same
 * {@code org.springframework.stereotype.Controller} user-class may have their own
 * {@link org.springframework.integration.http.inbound.RequestMapping}.
 * On the other side, all Spring Integration HTTP Inbound Endpoints are configured on
 * the basis of the same {@link org.springframework.integration.http.inbound.HttpRequestHandlingEndpointSupport}
 * class and there is no single {@link org.springframework.web.reactive.result.method.RequestMappingInfo}
 * configuration without {@link org.springframework.web.method.HandlerMethod} in Spring MVC.
 * Accordingly {@link WebFluxIntegrationRequestMappingHandlerMapping} is a
 * {@link org.springframework.web.reactive.HandlerMapping}
 * compromise implementation between method-level annotations and component-level
 * (e.g. Spring Integration XML) configurations.
 * <p>
 * Starting with version 5.1, this class implements {@link DestructionAwareBeanPostProcessor} to
 * register HTTP endpoints at runtime for dynamically declared beans, e.g. via
 * {@link org.springframework.integration.dsl.context.IntegrationFlowContext}, and unregister
 * them during the {@link WebFluxInboundEndpoint} destruction.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 *
 * @see org.springframework.integration.http.inbound.RequestMapping
 * @see RequestMappingHandlerMapping
 */
public class WebFluxIntegrationRequestMappingHandlerMapping extends RequestMappingHandlerMapping
		implements ApplicationListener<ContextRefreshedEvent>, DestructionAwareBeanPostProcessor {

	private static final Method HANDLER_METHOD =
			ReflectionUtils.findMethod(WebHandler.class, "handle", ServerWebExchange.class);

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
			unregisterMapping(getMappingForEndpoint((WebFluxInboundEndpoint) bean));
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return isHandler(bean.getClass());
	}

	@Override
	protected boolean isHandler(Class<?> beanType) {
		return WebFluxInboundEndpoint.class.isAssignableFrom(beanType);
	}

	@Override
	protected void detectHandlerMethods(Object handler) {
		if (handler instanceof String) {
			handler = getApplicationContext().getBean((String) handler); // NOSONAR never null
		}
		RequestMappingInfo mapping = getMappingForEndpoint((WebFluxInboundEndpoint) handler);
		if (mapping != null) {
			registerMapping(mapping, handler, HANDLER_METHOD); // NOSONAR never null
		}
	}

	/**
	 * Create a {@link RequestMappingInfo} from
	 * a Spring Integration Reactive HTTP Inbound Endpoint
	 * {@link org.springframework.integration.http.inbound.RequestMapping}.
	 * @see RequestMappingHandlerMapping#getMappingForMethod
	 */
	private RequestMappingInfo getMappingForEndpoint(WebFluxInboundEndpoint endpoint) {
		org.springframework.web.bind.annotation.RequestMapping requestMappingAnnotation =
				HttpContextUtils.convertRequestMappingToAnnotation(endpoint.getRequestMapping());
		if (requestMappingAnnotation != null) {
			return createRequestMappingInfo(requestMappingAnnotation, getCustomTypeCondition(endpoint.getClass()));
		}
		else {
			return null;
		}
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
		CrossOrigin crossOrigin = ((BaseHttpInboundEndpoint) handler).getCrossOrigin();
		if (crossOrigin != null) {
			CorsConfiguration config = new CorsConfiguration();
			for (RequestMethod requestMethod : crossOrigin.getMethod()) {
				config.addAllowedMethod(requestMethod.name());
			}
			config.setAllowedOrigins(Arrays.asList(crossOrigin.getOrigin()));
			config.setAllowedHeaders(Arrays.asList(crossOrigin.getAllowedHeaders()));
			config.setExposedHeaders(Arrays.asList(crossOrigin.getExposedHeaders()));
			config.setAllowCredentials(crossOrigin.getAllowCredentials());
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
		return null;
	}

	/**
	 * {@link org.springframework.integration.http.inbound.HttpRequestHandlingEndpointSupport}s
	 * may depend on auto-created {@code requestChannel}s, so MVC Handlers detection should be postponed
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
