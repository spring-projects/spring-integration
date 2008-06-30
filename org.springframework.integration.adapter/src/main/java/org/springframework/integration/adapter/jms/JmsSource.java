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

import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageSource;
import org.springframework.jms.core.JmsTemplate;

/**
 * A source for receiving JMS Messages with a polling listener. This source is
 * only recommended for very low message volume. Otherwise, the
 * {@link JmsGateway} that uses Spring's MessageListener
 * container support is highly recommended.
 * 
 * @author Mark Fisher
 */
public class JmsSource extends AbstractJmsTemplateBasedAdapter implements MessageSource<Object> {

	public JmsSource(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

	public JmsSource(ConnectionFactory connectionFactory, Destination destination) {
		super(connectionFactory, destination);
	}

	public JmsSource(ConnectionFactory connectionFactory, String destinationName) {
		super(connectionFactory, destinationName);
	}


	public Message<Object> receive() {
		Object receivedObject = this.getJmsTemplate().receiveAndConvert();
		if (receivedObject == null) {
			return null;
		}
		if (receivedObject instanceof Message) {
			return (Message<Object>) receivedObject;
		}
		return new GenericMessage<Object>(receivedObject);
	}

}
