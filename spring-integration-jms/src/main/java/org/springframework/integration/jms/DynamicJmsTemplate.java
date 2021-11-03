/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jms;

import jakarta.jms.ConnectionFactory;

import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.JmsDestinationAccessor;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0.2
 */
public class DynamicJmsTemplate extends JmsTemplate {

	private static final long NO_CACHING_RECEIVE_TIMEOUT = 1000L;

	private boolean receiveTimeoutExplicitlySet;

	@Override
	public void setReceiveTimeout(long receiveTimeout) {
		super.setReceiveTimeout(receiveTimeout);
		this.receiveTimeoutExplicitlySet = true;
	}

	@Override
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		super.setConnectionFactory(connectionFactory);
		if (!this.receiveTimeoutExplicitlySet) {
			if (connectionFactory instanceof CachingConnectionFactory &&
					((CachingConnectionFactory) connectionFactory).isCacheConsumers()) {
				super.setReceiveTimeout(JmsDestinationAccessor.RECEIVE_TIMEOUT_NO_WAIT);
			}
			else {
				super.setReceiveTimeout(NO_CACHING_RECEIVE_TIMEOUT);
			}
		}
	}

	@Override
	public int getPriority() {
		Integer priority = DynamicJmsTemplateProperties.getPriority();
		if (priority == null) {
			return super.getPriority();
		}
		Assert.isTrue(priority >= 0 && priority <= 9, "JMS priority must be in the range of 0-9"); // NOSONAR magic number
		return priority;
	}

	@Override
	public long getReceiveTimeout() {
		Long receiveTimeout = DynamicJmsTemplateProperties.getReceiveTimeout();
		return (receiveTimeout != null) ? receiveTimeout : super.getReceiveTimeout();
	}

	@Override
	public int getDeliveryMode() {
		Integer deliveryMode = DynamicJmsTemplateProperties.getDeliveryMode();
		return (deliveryMode != null) ? deliveryMode : super.getDeliveryMode();
	}

	@Override
	public long getTimeToLive() {
		Long timeToLive = DynamicJmsTemplateProperties.getTimeToLive();
		return (timeToLive != null) ? timeToLive : super.getTimeToLive();
	}

}
