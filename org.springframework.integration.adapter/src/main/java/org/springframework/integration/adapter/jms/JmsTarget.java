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

package org.springframework.integration.adapter.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.jms.core.JmsTemplate;

/**
 * A target for sending JMS Messages.
 * 
 * @author Mark Fisher
 */
public class JmsTarget extends AbstractJmsTemplateBasedAdapter implements MessageTarget {

	public JmsTarget(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

	public JmsTarget(ConnectionFactory connectionFactory, Destination destination) {
		super(connectionFactory, destination);
	}

	public JmsTarget(ConnectionFactory connectionFactory, String destinationName) {
		super(connectionFactory, destinationName);
	}

	public JmsTarget() {
		super();
	}


	public final boolean send(final Message<?> message) {
		if (message == null) {
			throw new IllegalArgumentException("message must not be null");
		}
		this.getJmsTemplate().convertAndSend(message);
		return true;
	}

}
