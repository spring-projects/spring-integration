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
import org.springframework.integration.annotation.ChannelAdapter;
import org.springframework.integration.annotation.Pollable;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.PollableChannelAdapter;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MethodInvokingSource;

/**
 * Post-processor for methods annotated with {@link Pollable @Pollable}.
 *
 * @author Mark Fisher
 */
public class PollableAnnotationPostProcessor extends AbstractAnnotationMethodPostProcessor<MessageSource<?>> {

	public PollableAnnotationPostProcessor(MessageBus messageBus, ClassLoader beanClassLoader) {
		super(Pollable.class, messageBus, beanClassLoader);
	}


	protected MessageSource<?> processMethod(Object bean, Method method, Annotation annotation) {
		MethodInvokingSource source = new MethodInvokingSource();
		source.setObject(bean);
		source.setMethodName(method.getName());
		ChannelAdapter channelAdapterAnnotation = AnnotationUtils.findAnnotation(bean.getClass(), ChannelAdapter.class);
		if (channelAdapterAnnotation != null) {
			String channelName = channelAdapterAnnotation.value();
			PollableChannelAdapter adapter = new PollableChannelAdapter(channelName, source, null);
			this.getMessageBus().registerChannel(channelName, adapter);
		}
		return source;
	}

	protected MessageSource<?> processResults(List<MessageSource<?>> results) {
		if (results.size() > 1) {
			throw new ConfigurationException("At most one @Pollable annotation is allowed per class.");
		}
		return (results.size() == 1) ? results.get(0) : null;
	}

	public AbstractEndpoint createEndpoint(Object bean, String beanName, Class<?> originalBeanClass,
			org.springframework.integration.annotation.MessageEndpoint endpointAnnotation) {
		return null;
	}

}
