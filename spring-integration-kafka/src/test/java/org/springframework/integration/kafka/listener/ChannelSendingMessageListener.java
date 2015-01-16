/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.integration.kafka.listener;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.listener.MessageListener;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;

/**
 * @author Marius Bogoevici
 */
public class ChannelSendingMessageListener implements MessageListener, ApplicationContextAware {

	private String channelName;

	private MessageChannel messageChannel;

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	DestinationResolver<MessageChannel> destinationResolver;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.destinationResolver = new BeanFactoryChannelResolver(applicationContext);
		messageChannel = destinationResolver.resolveDestination(channelName);
	}

	@Override
	public void onMessage(KafkaMessage message) {
		byte b[] = new byte[message.getMessage().payloadSize()];
		message.getMessage().payload().get(b);
		messageChannel.send(MessageBuilder.withPayload(new String(b)).build());
	}
}
