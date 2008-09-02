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
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.OutboundChannelAdapter;
import org.springframework.integration.handler.MethodInvokingTarget;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.scheduling.PollingSchedule;

/**
 * Post-processor for classes annotated with {@link MessageTarget @MessageTarget}.
 *
 * @author Mark Fisher
 */
public class TargetAnnotationPostProcessor extends AbstractAnnotationMethodPostProcessor<MessageTarget> {

	public TargetAnnotationPostProcessor(MessageBus messageBus, ClassLoader beanClassLoader) {
		super(org.springframework.integration.annotation.MessageTarget.class, messageBus, beanClassLoader);
	}


	protected MessageTarget processMethod(Object bean, Method method, Annotation annotation) {
		MethodInvokingTarget target = new MethodInvokingTarget(bean, method);
		ChannelAdapter channelAdapterAnnotation = AnnotationUtils.findAnnotation(bean.getClass(), ChannelAdapter.class);
		if (channelAdapterAnnotation != null) {
			OutboundChannelAdapter adapter = new OutboundChannelAdapter();
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
			if (channel instanceof PollableSource) {
				// TODO: add poller config if period, etc is provided (add to @Pollable)
				PollingDispatcher poller = new PollingDispatcher((PollableSource<?>) channel, new PollingSchedule(0));
				adapter.setSource(poller);
			}
			else {
				adapter.setSource(channel);
			}
			adapter.setTarget(target);
			this.getMessageBus().registerEndpoint(adapter);
		}
		return target;
	}

	protected MessageTarget processResults(List<MessageTarget> results) {
		if (results.size() > 1) {
			throw new ConfigurationException("At most one @MessageTarget annotation is allowed per class.");
		}
		return (results.size() == 1) ? results.get(0) : null;
	}

	public AbstractEndpoint createEndpoint(Object bean) {
		return null;
	}

}
