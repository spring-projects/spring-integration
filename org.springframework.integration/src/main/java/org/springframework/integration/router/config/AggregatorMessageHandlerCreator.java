/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.router.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.CompletionStrategy;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.config.AbstractMessageHandlerCreator;
import org.springframework.integration.router.AggregatingMessageHandler;
import org.springframework.integration.router.AggregatorAdapter;
import org.springframework.integration.router.CompletionStrategyAdapter;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Creates an {@link AggregatorAdapter AggregatorAdapter} for methods that aggregate messages.
 *
 * @author Marius Bogoevici
 */
public class AggregatorMessageHandlerCreator extends AbstractMessageHandlerCreator {

	private static final String DISCARD_CHANNEL = "discardChannel";

	private static final String SEND_TIMEOUT = "sendTimeout";

	private static final String SEND_PARTIAL_RESULTS_ON_TIMEOUT = "sendPartialResultsOnTimeout";

	private static final String REAPER_INTERVAL = "reaperInterval";

	private static final String TIMEOUT = "timeout";

	private static final String TRACKED_CORRELATION_ID_CAPACITY = "trackedCorrelationIdCapacity";


	private final ChannelRegistry channelRegistry;


	public AggregatorMessageHandlerCreator(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}


	public MessageHandler doCreateHandler(Object object, Method method, Map<String, ?> attributes) {
		AggregatingMessageHandler messageHandler = new AggregatingMessageHandler(new AggregatorAdapter(object, method));
		this.configureDefaultReplyChannel(messageHandler, object);
		String discardChannelName = this.getAttribute(attributes, DISCARD_CHANNEL, String.class);
		if (discardChannelName != null) {
			messageHandler.setDiscardChannel(this.channelRegistry.lookupChannel(discardChannelName));
		}
		Long sendTimeout = this.getAttribute(attributes, SEND_TIMEOUT, Long.class);
		if (sendTimeout != null) {
			messageHandler.setSendTimeout(sendTimeout);
		}
		Boolean sendPartialResultOnTimeout = this.getAttribute(
				attributes, SEND_PARTIAL_RESULTS_ON_TIMEOUT, Boolean.class);
		if (sendPartialResultOnTimeout != null) {
			messageHandler.setSendPartialResultOnTimeout(sendPartialResultOnTimeout);
		}
		Long reaperInterval = this.getAttribute(attributes, REAPER_INTERVAL, Long.class);
		if (reaperInterval != null) {
			messageHandler.setReaperInterval(reaperInterval);
		}
		Long timeout = this.getAttribute(attributes, TIMEOUT, Long.class);
		if (timeout != null) {
			messageHandler.setTimeout(timeout);
		}
		Integer trackedCorrelationIdCapacity = this.getAttribute(
				attributes, TRACKED_CORRELATION_ID_CAPACITY, Integer.class);
		if (trackedCorrelationIdCapacity != null) {
			messageHandler.setTrackedCorrelationIdCapacity(trackedCorrelationIdCapacity);
		}
		this.configureCompletionStrategy(object, messageHandler);
		return messageHandler;
	}

	private void configureDefaultReplyChannel(AggregatingMessageHandler handler, Object originalObject) {
		MessageEndpoint endpointAnnotation = AnnotationUtils.findAnnotation(
				AopUtils.getTargetClass(originalObject), MessageEndpoint.class);
		if (endpointAnnotation != null) {
			String outputChannelName = endpointAnnotation.output();
			if (StringUtils.hasText(outputChannelName)) {
				handler.setOutputChannel(this.channelRegistry.lookupChannel(outputChannelName));
			}
		}
	}

	private void configureCompletionStrategy(final Object object, final AggregatingMessageHandler handler) {
		ReflectionUtils.doWithMethods(object.getClass(), new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation annotation = AnnotationUtils.getAnnotation(method, CompletionStrategy.class);
				if (annotation != null) {
					handler.setCompletionStrategy(new CompletionStrategyAdapter(object, method));
				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	private <T> T getAttribute(Map<String, ?> attributes, String name, Class<T> expectedType) {
		Object value = attributes.get(name);
		if (value == null || !expectedType.isAssignableFrom(value.getClass())) {
			return null;
		}
		if (value instanceof String && !StringUtils.hasText((String) value)) {
			return null;
		}
		return (T) value;
	}

}
