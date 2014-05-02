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
package org.springframework.integration.core;

import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
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
		this.setDefaultChannel(defaultChannel);
	}

	/**
	 * Overridden to set the destination resolver to a {@link BeanFactoryChannelResolver}.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setDestinationResolver(new BeanFactoryChannelResolver(beanFactory));
		Properties integrationProperties = IntegrationContextUtils.getIntegrationProperties(beanFactory);
		Boolean throwExceptionOnLateReply = Boolean.valueOf(integrationProperties.getProperty(IntegrationProperties.THROW_EXCEPTION_ON_LATE_REPLY));
		this.setThrowExceptionOnLateReply(throwExceptionOnLateReply);
	}

	/**
	 * Invokes {@code setDefaultDestination(MessageChannel)} - provided for
	 * backward compatibility.
	 * @param channel the channel to set.
	 */
	public void setDefaultChannel(MessageChannel channel) {
		super.setDefaultDestination(channel);
	}

}
