/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.Message;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.core.JmsTemplate;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class PollableJmsChannel extends AbstractJmsChannel implements PollableChannel {

	private Log logger = LogFactory.getLog(this.getClass());


	public PollableJmsChannel(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}


	public Message<?> receive() {
		Object object = this.getJmsTemplate().receiveAndConvert();
		if (object == null) {
			return null;
		}
		if (object instanceof Message<?>) {
			return (Message<?>) object;
		}
		return MessageBuilder.withPayload(object).build();
	}

	public Message<?> receive(long timeout) {
		if (logger.isWarnEnabled() && this.timeoutConflictsWithTemplateValue(timeout)) {
			logger.warn("The JmsTemplate's receiveTimeout value is always used for the JMS channel. " +
					"Its current value is " + this.getJmsTemplate().getReceiveTimeout() + 
					". The passed value of " + timeout + " will be ignored.");
		}
		return this.receive();
	}

	private boolean timeoutConflictsWithTemplateValue(long timeout) {
		long templateTimeout = this.getJmsTemplate().getReceiveTimeout();
		return (timeout == -1 && templateTimeout != JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT)
				|| (timeout == 0 && templateTimeout != JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT)
				|| (timeout != templateTimeout);
	}

}
