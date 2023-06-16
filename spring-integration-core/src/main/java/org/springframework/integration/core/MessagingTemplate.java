/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.core;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.support.channel.ChannelResolverUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.GenericMessagingTemplate;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @since 1.0
 *
 */
public class MessagingTemplate extends GenericMessagingTemplate {

	private final Lock lock = new ReentrantLock();

	private BeanFactory beanFactory;

	private volatile boolean throwExceptionOnLateReplySet;

	/**
	 * Create a MessagingTemplate with no default channel. Note, that one
	 * may be provided by invoking {@link #setDefaultChannel(MessageChannel)}.
	 */
	public MessagingTemplate() {
	}

	/**
	 * Create a MessagingTemplate with the given default channel.
	 * @param defaultChannel the default {@link MessageChannel} for {@code send} operations
	 */
	public MessagingTemplate(MessageChannel defaultChannel) {
		super.setDefaultDestination(defaultChannel);
	}

	/**
	 * Overridden to set the destination resolver to a {@code integrationChannelResolver} bean.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory; //NOSONAR - non-sync is ok here
		setDestinationResolver(ChannelResolverUtils.getChannelResolver(beanFactory));
	}

	@Override
	public void setThrowExceptionOnLateReply(boolean throwExceptionOnLateReply) {
		super.setThrowExceptionOnLateReply(throwExceptionOnLateReply);
		this.throwExceptionOnLateReplySet = true;
	}

	/**
	 * Invokes {@code setDefaultDestination(MessageChannel)} - provided for
	 * backward compatibility.
	 * @param channel the channel to set.
	 */
	public void setDefaultChannel(MessageChannel channel) {
		super.setDefaultDestination(channel);
	}

	@Override
	@Nullable
	public Message<?> sendAndReceive(MessageChannel destination, Message<?> requestMessage) {
		if (!this.throwExceptionOnLateReplySet) {
			this.lock.lock();
			try {
				if (!this.throwExceptionOnLateReplySet) {
					IntegrationProperties integrationProperties = IntegrationContextUtils
							.getIntegrationProperties(this.beanFactory);
					super.setThrowExceptionOnLateReply(
							integrationProperties.isMessagingTemplateThrowExceptionOnLateReply());
					this.throwExceptionOnLateReplySet = true;
				}
			}
			finally {
				this.lock.unlock();
			}
		}
		return super.sendAndReceive(destination, requestMessage);
	}

	public Object receiveAndConvert(MessageChannel destination, long timeout) {
		Message<?> message = doReceive(destination, timeout);
		if (message != null) {
			return doConvert(message, Object.class);
		}
		else {
			return null;
		}
	}

	public Message<?> receive(MessageChannel destination, long timeout) {
		return doReceive(destination, timeout);
	}

}
