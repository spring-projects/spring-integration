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

import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.context.MessageHistoryWriter;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessageSource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * A source for receiving JMS Messages with a polling listener. This source is
 * only recommended for very low message volume. Otherwise, the
 * {@link JmsMessageDrivenEndpoint} that uses Spring's MessageListener container
 * support is a better option.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class JmsDestinationPollingSource extends AbstractJmsTemplateBasedAdapter implements MessageSource<Object> {

	private volatile String messageSelector;


	public JmsDestinationPollingSource(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

	public JmsDestinationPollingSource(ConnectionFactory connectionFactory, Destination destination) {
		super(connectionFactory, destination);
	}

	public JmsDestinationPollingSource(ConnectionFactory connectionFactory, String destinationName) {
		super(connectionFactory, destinationName);
	}


	public String getComponentType() {
		return "jms:inbound-channel-adapter";
	}

	/**
	 * Specify a JMS Message Selector expression to use when receiving Messages.
	 */
	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}
	
	/**
	 * Will receive a JMS {@link javax.jms.Message} converting and returning it as 
	 * a Spring Integration {@link Message}. This method will also use the current
	 * {@link JmsHeaderMapper} instance to map JMS properties to the MessageHeaders.
	 */
	@SuppressWarnings("unchecked")
	public Message<Object> receive() {
		Message<Object> convertedMessage = null;
		// receive JMS Message
		javax.jms.Message jmsMessage = this.getJmsTemplate().receiveSelected(this.messageSelector);
		if (jmsMessage == null) {
			return null;
		}
		try {	
			// Map headers
			Map<String, Object> mappedHeaders = (Map<String, Object>) this.getHeaderMapper().toHeaders(jmsMessage);
			MessageConverter converter = this.getJmsTemplate().getMessageConverter();
			Object convertedObject = converter.fromMessage(jmsMessage);
			MessageBuilder<Object> builder = (convertedObject instanceof Message)
					? MessageBuilder.fromMessage((Message<Object>) convertedObject) : MessageBuilder.withPayload(convertedObject);
			convertedMessage = builder.copyHeadersIfAbsent(mappedHeaders).build();
			convertedMessage = MessageHistoryWriter.writeHistory(this, convertedMessage);
		}
		catch (Exception e) {
			throw new MessagingException(e.getMessage(), e);
		}
		return convertedMessage;
	}

}
