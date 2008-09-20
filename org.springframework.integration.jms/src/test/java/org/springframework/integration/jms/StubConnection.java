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

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * @author Mark Fisher
 */
public class StubConnection implements Connection {

	private String messageText;


	public StubConnection(String messageText) {
		this.messageText = messageText;
	}


	public void close() throws JMSException {
	}

	public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector,
			ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return null;
	}

	public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName,
			String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return null;
	}

	public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
		return new StubSession(this.messageText);
	}

	public String getClientID() throws JMSException {
		return null;
	}

	public ExceptionListener getExceptionListener() throws JMSException {
		return null;
	}

	public ConnectionMetaData getMetaData() throws JMSException {
		return null;
	}

	public void setClientID(String clientID) throws JMSException {
	}

	public void setExceptionListener(ExceptionListener listener) throws JMSException {
	}

	public void start() throws JMSException {
	}

	public void stop() throws JMSException {
	}

}
