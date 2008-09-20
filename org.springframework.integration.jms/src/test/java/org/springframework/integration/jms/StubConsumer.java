/*
 * Copyright 2002-2007 the original author or authors.
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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

/**
 * @author Mark Fisher
 */
public class StubConsumer implements MessageConsumer {

	private String messageText;


	public StubConsumer(String messageText) {
		this.messageText = messageText;
	}


	public void close() throws JMSException {
	}

	public MessageListener getMessageListener() throws JMSException {
		return null;
	}

	public String getMessageSelector() throws JMSException {
		return null;
	}

	public Message receive() throws JMSException {
		StubTextMessage message = new StubTextMessage();
		message.setText(this.messageText);
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
