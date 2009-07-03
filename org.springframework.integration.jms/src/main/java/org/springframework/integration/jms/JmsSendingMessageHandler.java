/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.core.Ordered;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * A MessageConsumer that sends the converted Message payload within
 * a JMS Message.
 * 
 * @author Mark Fisher
 */
public class JmsSendingMessageHandler extends AbstractJmsTemplateBasedAdapter implements MessageHandler, Ordered {

	private volatile boolean extractPayload = true;

	private volatile int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * Specify whether the payload should be extracted from each Spring
	 * Integration Message to be converted to the body of a JMS Message.
	 * 
	 * <p>The default value is <code>true</code>. To force creation of JMS
	 * Messages whose body is the actual Spring Integration Message instance,
	 * set this to <code>false</code>.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return this.order;
	}

	public final void handleMessage(final Message<?> message) {
		if (message == null) {
			throw new IllegalArgumentException("message must not be null");
		}
		this.getJmsTemplate().convertAndSend(message);
	}

	@Override
	protected void configureMessageConverter(JmsTemplate jmsTemplate, JmsHeaderMapper headerMapper) {
		MessageConverter converter = jmsTemplate.getMessageConverter();
		if (converter == null || !(converter instanceof HeaderMappingMessageConverter)) {
			HeaderMappingMessageConverter hmmc = new HeaderMappingMessageConverter(converter, headerMapper);
			hmmc.setExtractIntegrationMessagePayload(this.extractPayload);
			jmsTemplate.setMessageConverter(hmmc);
		}
	}

}
