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

package org.springframework.integration.config.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.AggregatorAdapter;
import org.springframework.integration.aggregator.CompletionStrategyAdapter;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.CompletionStrategy;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.DefaultEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Post-processor for the {@link Aggregator @Aggregator} annotation.
 * 
 * @author Mark Fisher
 */
public class AggregatorAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Aggregator> {

	public AggregatorAnnotationPostProcessor(MessageBus messageBus) {
		super(messageBus);
	}


	@Override
	protected Object createMethodInvokingAdapter(Object bean, Method method, Aggregator annotation) {
		Aggregator aggregatorAnnotation = (Aggregator) annotation;
		AggregatingMessageHandler messageHandler = new AggregatingMessageHandler(new AggregatorAdapter(bean, method));
		String outputChannelName = aggregatorAnnotation.outputChannel();
		if (StringUtils.hasText(outputChannelName)) {
			messageHandler.setOutputChannel(this.getChannelRegistry().lookupChannel(outputChannelName));
		}
		String discardChannelName = aggregatorAnnotation.discardChannel();
		if (StringUtils.hasText(discardChannelName)) {
			messageHandler.setDiscardChannel(this.getChannelRegistry().lookupChannel(discardChannelName));
		}
		messageHandler.setSendTimeout(aggregatorAnnotation.sendTimeout());
		messageHandler.setSendPartialResultOnTimeout(aggregatorAnnotation.sendPartialResultsOnTimeout());
		messageHandler.setReaperInterval(aggregatorAnnotation.reaperInterval());
		messageHandler.setTimeout(aggregatorAnnotation.timeout());
		messageHandler.setTrackedCorrelationIdCapacity(aggregatorAnnotation.trackedCorrelationIdCapacity());
		this.configureCompletionStrategy(bean, messageHandler);
		messageHandler.afterPropertiesSet();
		return messageHandler;
	}

	@Override
	protected AbstractEndpoint createEndpoint(Object adapter) {
		if (adapter instanceof MessageHandler) {
			return new DefaultEndpoint<MessageHandler>((MessageHandler) adapter);
		}
		return null;
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
