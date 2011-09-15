/*
 * Copyright 2001-2011 the original author or authors.
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

package org.springframework.integration.ip.tcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpListener;
import org.springframework.integration.ip.tcp.connection.TcpSender;
import org.springframework.util.Assert;

/** 
 * TCP outbound gateway that uses a client connection factory. If the factory is configured
 * for single-use connections, each request is sent on a new connection; if the factory does not use
 * single use connections, each request is blocked until the previous response is received
 * (or times out). Asynchronous requests/responses over the same connection are not 
 * supported - use a pair of outbound/inbound adapters for that use case.
 * 
 * @author Gary Russell
 * @since 2.0
 */
public class TcpOutboundGateway extends AbstractReplyProducingMessageHandler implements TcpSender, TcpListener {

	protected AbstractConnectionFactory connectionFactory;
	
	private Map<String, AsyncReply> pendingReplies = new ConcurrentHashMap<String, AsyncReply>();
	
	private Semaphore semaphore = new Semaphore(1, true);

	private long replyTimeout = 10000;
	
	private long requestTimeout = 10000;


	/**
	 * @param requestTimeout the requestTimeout to set
	 */
	public void setRequestTimeout(long requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	/**
	 * @param replyTimeout the replyTimeout to set
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.replyTimeout = replyTimeout;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Assert.notNull(connectionFactory, this.getClass().getName() +
				" requires a client connection factory");
		boolean haveSemaphore = false;
		try {
			boolean singleUseConnection = this.connectionFactory.isSingleUse();
			if (!singleUseConnection) {
				logger.debug("trying semaphore");
				if (!this.semaphore.tryAcquire(this.requestTimeout, TimeUnit.MILLISECONDS)) {
					throw new MessageTimeoutException(requestMessage, "Timed out waiting for connection");
				}
				haveSemaphore = true;
				if (logger.isDebugEnabled()) {
					logger.debug("got semaphore");
				}
			}
			TcpConnection connection = this.connectionFactory.getConnection();
			AsyncReply reply = new AsyncReply();
			pendingReplies.put(connection.getConnectionId(), reply);
			if (logger.isDebugEnabled()) {
				logger.debug("Added " + connection.getConnectionId());
			}
			connection.send(requestMessage);
			Message<?> replyMessage = reply.getReply();
			if (replyMessage == null) {
				throw new MessageTimeoutException(requestMessage, "Timed out waiting for response");				
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Respose " + replyMessage);
			}
			return replyMessage;
		}
		catch (Exception e) {
			if (e instanceof MessagingException) {
				throw (MessagingException) e;
			}
			logger.error("Tcp Gateway exception", e);
			throw new MessagingException("Failed to send or receive", e);
		}
		finally {
			if (haveSemaphore) {
				this.semaphore.release();
				if (logger.isDebugEnabled()) {
					logger.debug("released semaphore");
				}
			}
		}
	}

	public boolean onMessage(Message<?> message) {
		String connectionId = (String) message.getHeaders().get(IpHeaders.CONNECTION_ID);
		if (connectionId == null) {
			logger.error("Cannot correlate response - no connection id");
			return false;
		}
		AsyncReply reply = pendingReplies.get(connectionId);
		if (reply == null) {
			logger.error("Cannot correlate response - no pending reply");
			return false;
		}
		reply.setReply(message);
		return false;
	}

	public void setConnectionFactory(AbstractConnectionFactory connectionFactory) {
		Assert.isTrue(connectionFactory instanceof AbstractClientConnectionFactory, 
				this.getClass().getName() + " requires a client connection factory");
		this.connectionFactory = connectionFactory;
		connectionFactory.registerListener(this);
		connectionFactory.registerSender(this);
	}

	public void addNewConnection(TcpConnection connection) {
		// do nothing - no asynchronous multiplexing supported
	}

	public void removeDeadConnection(TcpConnection connection) {
		// do nothing - no asynchronous multiplexing supported
	}


	/**
	 * Class used to coordinate the asynchronous reply to its request.
	 * 
	 * @author Gary Russell
	 * @since 2.0
	 */
	private class AsyncReply {

		private final CountDownLatch latch;

		private volatile Message<?> reply;

		public AsyncReply() {
			this.latch = new CountDownLatch(1);
		}

		/**
		 * Sender blocks here until the reply is received, or we time out
		 * @return The return message or null if we time out
		 * @throws Exception
		 */
		public Message<?> getReply() throws Exception {
			try {
				if (!this.latch.await(replyTimeout, TimeUnit.MILLISECONDS)) {
					return null;
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return this.reply;
		}

		public void setReply(Message<?> reply) {
			this.reply = reply;
			this.latch.countDown();
		}
	}

	/**
	 * Specify the Spring Integration reply channel. If this property is not
	 * set the gateway will check for a 'replyChannel' header on the request.
	 */
	public void setReplyChannel(MessageChannel replyChannel) {
		this.setOutputChannel(replyChannel);
	}
	public String getComponentType(){
		return "ip:tcp-outbound-gateway";
	}
}
