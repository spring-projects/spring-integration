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
import org.springframework.integration.aggregator.AggregatorEndpoint;
import org.springframework.integration.aggregator.CompletionStrategyAdapter;
import org.springframework.integration.aggregator.MethodInvokingAggregator;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.CompletionStrategy;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.util.Assert;
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
		return new MethodInvokingAggregator(bean, method);
	}

	@Override
	protected AbstractEndpoint createEndpoint(Object originalBean, Object adapter) {
		if (adapter instanceof org.springframework.integration.aggregator.Aggregator) {
			AggregatorEndpoint endpoint = new AggregatorEndpoint((org.springframework.integration.aggregator.Aggregator) adapter);
			this.configureCompletionStrategy(originalBean, endpoint);
			return endpoint;
		}
		return null;
	}

	@Override
	protected void configureEndpoint(AbstractEndpoint endpoint, Aggregator annotation, Poller pollerAnnotation) {
		super.configureEndpoint(endpoint, annotation, pollerAnnotation);
		AggregatorEndpoint aggregatorEndpoint = (AggregatorEndpoint) endpoint;
		String discardChannelName = annotation.discardChannel();
		if (StringUtils.hasText(discardChannelName)) {
			MessageChannel discardChannel = this.getChannelRegistry().lookupChannel(discardChannelName);
			Assert.notNull(discardChannel, "unable to resolve discardChannel '" + discardChannelName + "'");
			aggregatorEndpoint.setDiscardChannel(discardChannel);
		}
		aggregatorEndpoint.setSendTimeout(annotation.sendTimeout());
		aggregatorEndpoint.setSendPartialResultOnTimeout(annotation.sendPartialResultsOnTimeout());
		aggregatorEndpoint.setReaperInterval(annotation.reaperInterval());
		aggregatorEndpoint.setTimeout(annotation.timeout());
		aggregatorEndpoint.setTrackedCorrelationIdCapacity(annotation.trackedCorrelationIdCapacity());
		aggregatorEndpoint.afterPropertiesSet();
	}

	private void configureCompletionStrategy(final Object object, final AggregatorEndpoint handler) {
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
