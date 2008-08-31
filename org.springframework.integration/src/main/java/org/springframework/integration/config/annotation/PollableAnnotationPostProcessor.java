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
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.InboundChannelAdapter;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MethodInvokingSource;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.scheduling.PollingSchedule;

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
		source.setMethod(method);
		ChannelAdapter channelAdapterAnnotation = AnnotationUtils.findAnnotation(bean.getClass(), ChannelAdapter.class);
		if (channelAdapterAnnotation != null) {
			Poller pollerAnnotation = AnnotationUtils.findAnnotation(bean.getClass(), Poller.class);
			if (pollerAnnotation == null) {
				throw new ConfigurationException("The @Poller annotation is required (at class-level) "
						+ "when using the @ChannelAdapter annotation with a @Pollable method annotation.");
			}
			PollingSchedule schedule = new PollingSchedule(pollerAnnotation.period());
			schedule.setInitialDelay(pollerAnnotation.initialDelay());
			schedule.setFixedRate(pollerAnnotation.fixedRate());
			schedule.setTimeUnit(pollerAnnotation.timeUnit());
			PollingDispatcher poller = new PollingDispatcher((PollableSource<?>) source, schedule);
			int maxMessagesPerPoll = pollerAnnotation.maxMessagesPerPoll();
			if (maxMessagesPerPoll == -1) {
				// the default is 1 since a MethodInvokingSource might return a non-null value
				// every time it is invoked, thus producing an infinite number of messages per poll
				maxMessagesPerPoll = 1;
			}
			poller.setMaxMessagesPerPoll(maxMessagesPerPoll);
			InboundChannelAdapter adapter = new InboundChannelAdapter();
			adapter.setSource(poller);
			String channelName = channelAdapterAnnotation.value();
			MessageChannel channel = this.getMessageBus().lookupChannel(channelName);
			if (channel == null) {
				adapter.setBeanName(channelName + ".adapter");
				DirectChannel directChannel = new DirectChannel();
				directChannel.setBeanName(channelName);
				this.getMessageBus().registerChannel(directChannel);
				channel = directChannel;
			}
			else {
				adapter.setBeanName(channelName);
			}
			adapter.setTarget(channel);
			this.getMessageBus().registerEndpoint(adapter);
		}
		return source;
	}

	protected MessageSource<?> processResults(List<MessageSource<?>> results) {
		if (results.size() > 1) {
			throw new ConfigurationException("At most one @Pollable annotation is allowed per class.");
		}
		return (results.size() == 1) ? results.get(0) : null;
	}

	public AbstractEndpoint createEndpoint(Object bean) {
		return null;
	}

}
