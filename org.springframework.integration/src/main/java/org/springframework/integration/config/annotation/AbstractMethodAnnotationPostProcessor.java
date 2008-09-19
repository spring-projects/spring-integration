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
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.AbstractMessageHandlingEndpoint;
import org.springframework.integration.endpoint.AbstractMessageConsumingEndpoint;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for Method-level annotation post-processors.
 *
 * @author Mark Fisher
 */
public abstract class AbstractMethodAnnotationPostProcessor<T extends Annotation> implements MethodAnnotationPostProcessor<T> {

	private static final String INPUT_CHANNEL_ATTRIBUTE = "inputChannel";

	private static final String OUTPUT_CHANNEL_ATTRIBUTE = "outputChannel";


	private final MessageBus messageBus;


	public AbstractMethodAnnotationPostProcessor(MessageBus messageBus) {
		Assert.notNull(messageBus, "MessageBus must not be null");
		this.messageBus = messageBus;
	}


	protected ChannelRegistry getChannelRegistry() {
		return this.messageBus;
	}

	public Object postProcess(Object bean, String beanName, Method method, T annotation) {
		Object adapter = this.createMethodInvokingAdapter(bean, method, annotation);
		if (adapter != null && this.shouldCreateEndpoint(annotation)) {
			AbstractEndpoint endpoint = this.createEndpoint(bean, adapter);
			if (endpoint != null) {
				Poller pollerAnnotation = AnnotationUtils.findAnnotation(method, Poller.class);
				this.configureEndpoint(endpoint, annotation, pollerAnnotation);
				endpoint.afterPropertiesSet();
				return endpoint;
			}
		}
		return adapter;
	}

	protected boolean shouldCreateEndpoint(T annotation) {
		return (StringUtils.hasText((String) AnnotationUtils.getValue(annotation, INPUT_CHANNEL_ATTRIBUTE)));
	}

	protected void configureEndpoint(AbstractEndpoint endpoint, T annotation, Poller pollerAnnotation) {
		String inputChannelName = (String) AnnotationUtils.getValue(annotation, INPUT_CHANNEL_ATTRIBUTE);
		if (StringUtils.hasText(inputChannelName)) {
			MessageChannel inputChannel = this.messageBus.lookupChannel(inputChannelName);
			if (inputChannel == null) {
				throw new ConfigurationException("unable to resolve inputChannel '" + inputChannelName + "'");
			}
			if (endpoint instanceof AbstractMessageConsumingEndpoint) {
				AbstractMessageConsumingEndpoint consumingEndpoint = (AbstractMessageConsumingEndpoint) endpoint;
				if (pollerAnnotation != null) {	
					if (inputChannel instanceof PollableChannel) {
						PollingSchedule schedule = new PollingSchedule(pollerAnnotation.period());
						schedule.setInitialDelay(pollerAnnotation.initialDelay());
						schedule.setFixedRate(pollerAnnotation.fixedRate());
						schedule.setTimeUnit(pollerAnnotation.timeUnit());
						consumingEndpoint.setSchedule(schedule);
						consumingEndpoint.setMaxMessagesPerPoll(pollerAnnotation.maxMessagesPerPoll());
					}
					else {
						throw new ConfigurationException("The @Poller annotation should only be provided for a PollableChannel");
					}
				}
				consumingEndpoint.setInputChannel(inputChannel);
			}
			if (endpoint instanceof AbstractMessageHandlingEndpoint) {
				String outputChannelName = (String) AnnotationUtils.getValue(annotation, OUTPUT_CHANNEL_ATTRIBUTE);
				if (StringUtils.hasText(outputChannelName)) {
					MessageChannel outputChannel = this.messageBus.lookupChannel(outputChannelName);
					if (outputChannel == null) {
						throw new ConfigurationException("unable to resolve outputChannel '" + outputChannelName + "'");
					}
					((AbstractMessageHandlingEndpoint) endpoint).setOutputChannel(outputChannel);
				}
			}
		}
	}

	protected abstract Object createMethodInvokingAdapter(Object bean, Method method, T annotation);

	protected abstract AbstractEndpoint createEndpoint(Object originalBean, Object adapter);

}
