/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.core;

import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.GenericMessagingTemplate;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 1.0
 *
 */
public class MessagingTemplate extends GenericMessagingTemplate {

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
	 * Overridden to set the destination resolver to a {@link BeanFactoryChannelResolver}.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory; //NOSONAR - non-sync is ok here
		super.setDestinationResolver(new BeanFactoryChannelResolver(beanFactory));
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
	public Message<?> sendAndReceive(MessageChannel destination, Message<?> requestMessage) {
		if (!this.throwExceptionOnLateReplySet) {
			synchronized (this) {
				if (!this.throwExceptionOnLateReplySet) {
					Properties integrationProperties =
							IntegrationContextUtils.getIntegrationProperties(this.beanFactory);
					Boolean throwExceptionOnLateReply = Boolean.valueOf(integrationProperties
							.getProperty(IntegrationProperties.THROW_EXCEPTION_ON_LATE_REPLY));
					super.setThrowExceptionOnLateReply(throwExceptionOnLateReply);
					this.throwExceptionOnLateReplySet = true;
				}
			}
		}
		return super.sendAndReceive(destination, requestMessage);
	}

}
