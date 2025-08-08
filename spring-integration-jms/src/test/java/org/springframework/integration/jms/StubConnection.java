/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionConsumer;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.Destination;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.ServerSessionPool;
import jakarta.jms.Session;
import jakarta.jms.Topic;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class StubConnection implements Connection {

	private String messageText;

	public StubConnection(String messageText) {
		this.messageText = messageText;
	}

	@Override
	public void close() throws JMSException {
	}

	@Override
	public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector,
			ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return null;
	}

	@Override
	public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName,
			String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return null;
	}

	@Override
	public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
		return new StubSession(this.messageText);
	}

	@Override
	public String getClientID() throws JMSException {
		return null;
	}

	@Override
	public ExceptionListener getExceptionListener() throws JMSException {
		return null;
	}

	@Override
	public ConnectionMetaData getMetaData() throws JMSException {
		return null;
	}

	@Override
	public void setClientID(String clientID) throws JMSException {
	}

	@Override
	public void setExceptionListener(ExceptionListener listener) throws JMSException {
	}

	@Override
	public void start() throws JMSException {
	}

	@Override
	public void stop() throws JMSException {
	}

	@Override
	public Session createSession(int sessionMode) throws JMSException {
		return new StubSession(this.messageText);
	}

	@Override
	public Session createSession() throws JMSException {
		return new StubSession(this.messageText);
	}

	@Override
	public ConnectionConsumer createSharedConnectionConsumer(Topic topic, String subscriptionName,
			String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return null;
	}

	@Override
	public ConnectionConsumer createSharedDurableConnectionConsumer(Topic topic, String subscriptionName,
			String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return null;
	}

}
