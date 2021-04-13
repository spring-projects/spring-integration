/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.integration.websocket.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.websocket.ServerWebSocketContainer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.ServletWebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.ServletWebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;

/**
 * The {@link ServletWebSocketHandlerRegistry} extension for Spring Integration purpose, especially
 * a dynamic WebSocket endpoint registrations.
 *
 * @author Artem Bilan
 *
 * @since 5.5
 */
class IntegrationServletWebSocketHandlerRegistry extends ServletWebSocketHandlerRegistry
		implements ApplicationContextAware, DestructionAwareBeanPostProcessor {

	private final Map<WebSocketHandler, List<String>> dynamicRegistrations = new HashMap<>();

	private ApplicationContext applicationContext;

	private volatile IntegrationDynamicWebSocketHandlerMapping dynamicHandlerMapping;

	IntegrationServletWebSocketHandlerRegistry() {
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}


	@Override
	protected boolean requiresTaskScheduler() { // NOSONAR visibility
		return super.requiresTaskScheduler();
	}

	@Override
	protected void setTaskScheduler(TaskScheduler scheduler) { // NOSONAR visibility
		super.setTaskScheduler(scheduler);
	}

	@Override
	public AbstractHandlerMapping getHandlerMapping() {
		AbstractHandlerMapping originHandlerMapping = super.getHandlerMapping();
		originHandlerMapping.setApplicationContext(this.applicationContext);
		this.dynamicHandlerMapping = this.applicationContext.getBean(IntegrationDynamicWebSocketHandlerMapping.class);
		return originHandlerMapping;
	}

	@Override
	public WebSocketHandlerRegistration addHandler(WebSocketHandler handler, String... paths) {
		if (this.dynamicHandlerMapping != null) {
			IntegrationDynamicWebSocketHandlerRegistration registration =
					new IntegrationDynamicWebSocketHandlerRegistration();
			registration.addHandler(handler, paths);
			MultiValueMap<HttpRequestHandler, String> mappings = registration.getMapping();
			for (Map.Entry<HttpRequestHandler, List<String>> entry : mappings.entrySet()) {
				HttpRequestHandler httpHandler = entry.getKey();
				List<String> patterns = entry.getValue();
				this.dynamicRegistrations.put(handler, patterns);
				for (String pattern : patterns) {
					this.dynamicHandlerMapping.registerHandler(pattern, httpHandler);
				}
			}
			return registration;
		}
		else {
			return super.addHandler(handler, paths);
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (this.dynamicHandlerMapping != null && bean instanceof ServerWebSocketContainer) {
			((ServerWebSocketContainer) bean).registerWebSocketHandlers(this);
		}
		return bean;
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return bean instanceof ServerWebSocketContainer;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (requiresDestruction(bean)) {
			removeRegistration((ServerWebSocketContainer) bean);
		}
	}

	void removeRegistration(ServerWebSocketContainer serverWebSocketContainer) {
		List<String> patterns = this.dynamicRegistrations.remove(serverWebSocketContainer.getWebSocketHandler());
		if (this.dynamicHandlerMapping != null && !CollectionUtils.isEmpty(patterns)) {
			for (String pattern : patterns) {
				this.dynamicHandlerMapping.unregisterHandler(pattern);
			}
		}
	}

	private static final class IntegrationDynamicWebSocketHandlerRegistration
			extends ServletWebSocketHandlerRegistration {

		MultiValueMap<HttpRequestHandler, String> getMapping() {
			return getMappings();
		}

	}

}
