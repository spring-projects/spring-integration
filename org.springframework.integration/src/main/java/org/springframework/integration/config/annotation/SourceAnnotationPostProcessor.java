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
import java.util.List;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.annotation.Polled;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.dispatcher.DirectChannel;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.SourceEndpoint;
import org.springframework.integration.message.MethodInvokingSource;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.util.StringUtils;

/**
 * Post-processor for classes annotated with {@link MessageSource @MessageSource}.
 *
 * @author Mark Fisher
 */
public class SourceAnnotationPostProcessor extends AbstractAnnotationMethodPostProcessor<MessageSource<?>> {

	public SourceAnnotationPostProcessor(MessageBus messageBus, ClassLoader beanClassLoader) {
		super(org.springframework.integration.annotation.MessageSource.class, messageBus, beanClassLoader);
	}


	protected MessageSource<?> processMethod(Object bean, Method method, Annotation annotation) {
		MethodInvokingSource source = new MethodInvokingSource();
		source.setObject(bean);
		source.setMethodName(method.getName());
		return source;
	}

	protected MessageSource<?> processResults(List<MessageSource<?>> results) {
		if (results.size() > 1) {
			throw new ConfigurationException("At most one @MessageSource annotation is allowed per class.");
		}
		return (results.size() == 1) ? results.get(0) : null;
	}

	public MessageEndpoint createEndpoint(Object bean, String beanName, Class<?> originalBeanClass,
			org.springframework.integration.annotation.MessageEndpoint endpointAnnotation) {
		SourceEndpoint endpoint = new SourceEndpoint((MessageSource<?>) bean);
		Polled polledAnnotation = AnnotationUtils.findAnnotation(originalBeanClass, Polled.class);
		int period = polledAnnotation.period();
		long initialDelay = polledAnnotation.initialDelay();
		boolean fixedRate = polledAnnotation.fixedRate();
		PollingSchedule schedule = new PollingSchedule(period);
		schedule.setInitialDelay(initialDelay);
		schedule.setFixedRate(fixedRate);
		endpoint.setSchedule(schedule);
		String outputChannelName = endpointAnnotation.output();
		if (!StringUtils.hasText(outputChannelName)) {
			MessageChannel outputChannel = new DirectChannel();
			this.getMessageBus().registerChannel(beanName + ".output", outputChannel);
			endpoint.setOutputChannel(outputChannel);
		}
		else {
			endpoint.setOutputChannelName(outputChannelName);
		}
		return endpoint;
	}

}
