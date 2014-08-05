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

package org.springframework.integration.jms;

import java.util.Map;

import javax.jms.Destination;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.jms.util.JmsAdapterUtils;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A source for receiving JMS Messages with a polling listener. This source is
 * only recommended for very low message volume. Otherwise, the
 * {@link JmsMessageDrivenEndpoint} that uses Spring's MessageListener container
 * support is a better option.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class JmsDestinationPollingSource extends IntegrationObjectSupport implements MessageSource<Object> {


	private final JmsTemplate jmsTemplate;

	private volatile Destination destination;

	private volatile String destinationName;

	private volatile String messageSelector;

	private volatile JmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();

	private volatile String sessionAcknowledgeMode;

	public JmsDestinationPollingSource(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public void setDestination(Destination destination) {
		Assert.isNull(this.destinationName, "The 'destination' and 'destinationName' properties are mutually exclusive.");
		this.destination = destination;
	}

	public void setDestinationName(String destinationName) {
		Assert.isNull(this.destination, "The 'destination' and 'destinationName' properties are mutually exclusive.");
		this.destinationName = destinationName;
	}

	@Override
	public String getComponentType() {
		return "jms:inbound-channel-adapter";
	}

	/**
	 * Specify a JMS Message Selector expression to use when receiving Messages.
	 *
	 * @param messageSelector The message selector.
	 */
	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	public void setSessionAcknowledgeMode(String sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
	}

	/**
	 * Will receive a JMS {@link javax.jms.Message} converting and returning it as
	 * a Spring Integration {@link Message}. This method will also use the current
	 * {@link JmsHeaderMapper} instance to map JMS properties to the MessageHeaders.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Message<Object> receive() {
		Message<Object> convertedMessage = null;
		javax.jms.Message jmsMessage = this.doReceiveJmsMessage();
		if (jmsMessage == null) {
			return null;
		}
		try {
			// Map headers
			Map<String, Object> mappedHeaders = this.headerMapper.toHeaders(jmsMessage);
			MessageConverter converter = this.jmsTemplate.getMessageConverter();
			Object convertedObject = converter.fromMessage(jmsMessage);
			AbstractIntegrationMessageBuilder<Object> builder = (convertedObject instanceof Message) ?
					this.getMessageBuilderFactory().fromMessage((Message<Object>) convertedObject) :
					this.getMessageBuilderFactory().withPayload(convertedObject);
			convertedMessage = builder.copyHeadersIfAbsent(mappedHeaders).build();
		}
		catch (Exception e) {
			throw new MessagingException(e.getMessage(), e);
		}
		return convertedMessage;
	}

	private javax.jms.Message doReceiveJmsMessage() {
		javax.jms.Message jmsMessage = null;
		if (this.destination != null) {
			jmsMessage = this.jmsTemplate.receiveSelected(this.destination, this.messageSelector);
		}
		else if (this.destinationName != null) {
			jmsMessage = this.jmsTemplate.receiveSelected(this.destinationName, this.messageSelector);
		}
		else {
			jmsMessage = this.jmsTemplate.receiveSelected(this.messageSelector);
		}
		return jmsMessage;
	}

	@Override
	protected void onInit() {
		if (this.sessionAcknowledgeMode != null) {
			Integer acknowledgeMode = JmsAdapterUtils.parseAcknowledgeMode(this.sessionAcknowledgeMode);
			if (acknowledgeMode != null) {
				if (JmsAdapterUtils.SESSION_TRANSACTED == acknowledgeMode) {
					this.jmsTemplate.setSessionTransacted(true);
				}
				else {
					this.jmsTemplate.setSessionAcknowledgeMode(acknowledgeMode);
				}
			}
		}
	}

}
