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

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.annotation.ChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.endpoint.InboundChannelAdapter;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.OutboundChannelAdapter;
import org.springframework.integration.handler.MethodInvokingTarget;
import org.springframework.integration.message.MethodInvokingSource;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * Post-processor for methods annotated with {@link ChannelAdapter @ChannelAdapter}.
 *
 * @author Mark Fisher
 */
public class ChannelAdapterAnnotationPostProcessor implements MethodAnnotationPostProcessor<ChannelAdapter> {

	private final MessageBus messageBus;


	public ChannelAdapterAnnotationPostProcessor(MessageBus messageBus) {
		Assert.notNull(messageBus, "MessageBus must not be null");
		this.messageBus = messageBus;
	}


	public Object postProcess(Object bean, String beanName, Method method, ChannelAdapter annotation) {
		MessageEndpoint endpoint = null;
		String channelName = annotation.value();
		MessageChannel channel = this.messageBus.lookupChannel(channelName);
		if (channel == null) {
			DirectChannel directChannel = new DirectChannel();
			directChannel.setBeanName(channelName);
			this.messageBus.registerChannel(directChannel);
			channel = directChannel;
		}
		Poller pollerAnnotation = AnnotationUtils.findAnnotation(method, Poller.class);
		if (method.getParameterTypes().length == 0 && hasReturnValue(method)) {
			MethodInvokingSource source = new MethodInvokingSource();
			source.setObject(bean);
			source.setMethod(method);
			endpoint = this.createInboundChannelAdapter(source, channel, pollerAnnotation);
		}
		else if (method.getParameterTypes().length > 0 && !hasReturnValue(method)) {
			MethodInvokingTarget target = new MethodInvokingTarget(bean, method);
			endpoint = this.createOutboundChannelAdapter(target, channel, pollerAnnotation);
		}
		else {
			throw new ConfigurationException("The @ChannelAdapter can only be applied to methods that accept no arguments but have"
					+ " a return value (inbound) or methods that have no return value but do accept arguments (outbound)");
		}
		if (endpoint != null) {
			this.messageBus.registerEndpoint(endpoint);
		}
		return bean;
	}

	private InboundChannelAdapter createInboundChannelAdapter(MethodInvokingSource source, MessageChannel channel, Poller pollerAnnotation) {
		if (pollerAnnotation == null) {
			throw new ConfigurationException("The @Poller annotation is required (at method-level) "
					+ "when using the @ChannelAdapter annotation with a no-arg method.");
		}
		Schedule schedule = this.createSchedule(pollerAnnotation);
		InboundChannelAdapter adapter = new InboundChannelAdapter();
		adapter.setSource(source);
		adapter.setOutputChannel(channel);
		adapter.setSchedule(schedule);
		adapter.setBeanName(this.generateUniqueName(channel.getName() + ".inboundAdapter"));
		return adapter;
	}

	private OutboundChannelAdapter createOutboundChannelAdapter(MethodInvokingTarget target, MessageChannel channel, Poller pollerAnnotation) {
		OutboundChannelAdapter adapter = new OutboundChannelAdapter(target);
		adapter.setInputChannel(channel);
		if (channel instanceof PollableChannel) {
			Schedule schedule = (pollerAnnotation != null)
					? this.createSchedule(pollerAnnotation)
					: new PollingSchedule(0);
			adapter.setSchedule(schedule);
		}
		adapter.setBeanName(this.generateUniqueName(channel.getName() + ".outboundAdapter"));
		return adapter;
	}

	private Schedule createSchedule(Poller pollerAnnotation) {
		PollingSchedule schedule = new PollingSchedule(pollerAnnotation.period());
		schedule.setInitialDelay(pollerAnnotation.initialDelay());
		schedule.setFixedRate(pollerAnnotation.fixedRate());
		schedule.setTimeUnit(pollerAnnotation.timeUnit());
		return schedule;
	}

	private boolean hasReturnValue(Method method) {
		return !method.getReturnType().equals(void.class);
	}

	private String generateUniqueName(String name) {
		int counter = 0;
		String id = name;
		while (this.messageBus.lookupEndpoint(id) != null) {
			id = name + "#" + counter;
			counter++;
		}
		return id;
	}

}
