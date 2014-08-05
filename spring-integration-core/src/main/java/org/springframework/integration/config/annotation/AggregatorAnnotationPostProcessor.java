/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.List;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy;
import org.springframework.integration.aggregator.MethodInvokingMessageGroupProcessor;
import org.springframework.integration.aggregator.MethodInvokingReleaseStrategy;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.CorrelationStrategy;
import org.springframework.integration.annotation.ReleaseStrategy;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

/**
 * Post-processor for the {@link Aggregator @Aggregator} annotation.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class AggregatorAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Aggregator> {

	public AggregatorAnnotationPostProcessor(ListableBeanFactory beanFactory, Environment environment) {
		super(beanFactory, environment);
	}


	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		MethodInvokingMessageGroupProcessor processor = new MethodInvokingMessageGroupProcessor(bean, method);
		processor.setBeanFactory(this.beanFactory);

		MethodInvokingReleaseStrategy releaseStrategy = null;
		Method releaseStrategyMethod = MessagingAnnotationUtils.findAnnotatedMethod(bean, ReleaseStrategy.class);
		if (releaseStrategyMethod != null) {
			releaseStrategy = new MethodInvokingReleaseStrategy(bean, releaseStrategyMethod);
		}

		MethodInvokingCorrelationStrategy correlationStrategy = null;
		Method correlationStrategyMethod = MessagingAnnotationUtils.findAnnotatedMethod(bean, CorrelationStrategy.class);
		if (correlationStrategyMethod != null) {
			correlationStrategy = new MethodInvokingCorrelationStrategy(bean, correlationStrategyMethod);
		}

		AggregatingMessageHandler handler = new AggregatingMessageHandler(processor, new SimpleMessageStore(),
				correlationStrategy, releaseStrategy);

		String discardChannelName = MessagingAnnotationUtils.resolveAttribute(annotations, "discardChannel", String.class);
		if (StringUtils.hasText(discardChannelName)) {
			handler.setDiscardChannelName(discardChannelName);
		}
		String outputChannelName = MessagingAnnotationUtils.resolveAttribute(annotations, "outputChannel", String.class);
		if (StringUtils.hasText(outputChannelName)) {
			handler.setOutputChannelName(outputChannelName);
		}
		Long sendTimeout = MessagingAnnotationUtils.resolveAttribute(annotations, "sendTimeout", Long.class);
		if (sendTimeout != null) {
			handler.setSendTimeout(sendTimeout);
		}
		Boolean sendPartialResultsOnExpiry = MessagingAnnotationUtils.resolveAttribute(annotations,
				"sendPartialResultsOnExpiry", Boolean.class);
		if (sendPartialResultsOnExpiry != null) {
			handler.setSendPartialResultOnExpiry(sendPartialResultsOnExpiry);
		}
		handler.setBeanFactory(this.beanFactory);
		return handler;
	}

	protected boolean beanAnnotationAware() {
		return false;
	}

}
