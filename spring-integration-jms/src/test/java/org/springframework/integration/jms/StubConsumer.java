/*
 * Copyright 2002-2024 the original author or authors.
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

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;

/**
 * @author Mark Fisher
 */
public class StubConsumer implements MessageConsumer {

	private String messageText;

	private String messageSelector;

	public StubConsumer(String messageText, String messageSelector) {
		this.messageText = messageText;
		this.messageSelector = messageSelector;
	}

	public void close() throws JMSException {
	}

	public MessageListener getMessageListener() throws JMSException {
		return null;
	}

	public String getMessageSelector() throws JMSException {
		return this.messageSelector;
	}

	public Message receive() throws JMSException {
		StubTextMessage message = new StubTextMessage();
		String text = this.messageText;
		if (this.messageSelector != null) {
			text += " [with selector: " + this.messageSelector + "]";
		}
		message.setText(text);
		return message;
	}

	public Message receive(long timeout) throws JMSException {
		return this.receive();
	}

	public Message receiveNoWait() throws JMSException {
		return this.receive();
	}

	public void setMessageListener(MessageListener listener) throws JMSException {
	}

}
