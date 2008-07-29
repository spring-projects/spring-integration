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
		if (attributes.containsKey(DISCARD_CHANNEL)) {
			messageHandler.setDiscardChannel(this.channelRegistry.lookupChannel(
					(String) attributes.get(DISCARD_CHANNEL)));
		}
		if (attributes.containsKey(SEND_TIMEOUT)) {
			messageHandler.setSendTimeout((Long) attributes.get(SEND_TIMEOUT));
		}
		if (attributes.containsKey(SEND_PARTIAL_RESULTS_ON_TIMEOUT)) {
			messageHandler.setSendPartialResultOnTimeout(
					(Boolean) attributes.get(SEND_PARTIAL_RESULTS_ON_TIMEOUT));
		}
		if (attributes.containsKey(REAPER_INTERVAL)) {
			messageHandler.setReaperInterval((Long) attributes.get(REAPER_INTERVAL));
		}
		if (attributes.containsKey(TIMEOUT)) {
			messageHandler.setTimeout((Long) attributes.get(TIMEOUT));
		}
		if (attributes.containsKey(TRACKED_CORRELATION_ID_CAPACITY)) {
			messageHandler.setTrackedCorrelationIdCapacity(
					(Integer) attributes.get(TRACKED_CORRELATION_ID_CAPACITY));
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

}
