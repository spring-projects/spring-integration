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

package org.springframework.integration.amqp.channel;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public abstract class AbstractAmqpChannel extends AbstractMessageChannel {

	private final AmqpTemplate amqpTemplate;


	AbstractAmqpChannel(AmqpTemplate amqpTemplate) {
		Assert.notNull(amqpTemplate, "amqpTemplate must not be null");
		this.amqpTemplate = amqpTemplate;
	}


	/**
	 * Subclasses may override this method to return an Exchange name.
	 * By default, Messages will be sent to the no-name Direct Exchange.
	 *
	 * @return The exchange name.
	 */
	protected String getExchangeName() {
		return "";
	}

	/**
	 * Subclasses may override this method to return a routing key.
	 * By default, there will be no routing key (empty string).
	 *
	 * @return The routing key.
	 */
	protected String getRoutingKey() {
		return "";
	}

	AmqpTemplate getAmqpTemplate() {
		return this.amqpTemplate;
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		this.amqpTemplate.convertAndSend(this.getExchangeName(), this.getRoutingKey(), message);
		return true;
	}

}
