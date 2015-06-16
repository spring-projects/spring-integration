/*
 * Copyright 2001-2015 the original author or authors.
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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpConnectionFailedCorrelationEvent;
import org.springframework.integration.ip.tcp.connection.TcpListener;
import org.springframework.integration.ip.tcp.connection.TcpSender;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;

/**
 * TCP outbound gateway that uses a client connection factory. If the factory is configured
 * for single-use connections, each request is sent on a new connection; if the factory does not use
 * single use connections, each request is blocked until the previous response is received
 * (or times out). Asynchronous requests/responses over the same connection are not
 * supported - use a pair of outbound/inbound adapters for that use case.
 * <p>
 * {@link SmartLifecycle} methods delegate to the underlying {@link AbstractConnectionFactory}
 *
 *
 * @author Gary Russell
 * @since 2.0
 */
public class TcpOutboundGateway extends AbstractReplyProducingMessageHandler
		implements TcpSender, TcpListener, IntegrationEvaluationContextAware, Lifecycle {

	private volatile AbstractClientConnectionFactory connectionFactory;

	private volatile boolean isSingleUse;

	private final Map<String, AsyncReply> pendingReplies = new ConcurrentHashMap<String, AsyncReply>();

	private final Semaphore semaphore = new Semaphore(1, true);

	private volatile Expression remoteTimeoutExpression = new LiteralExpression("10000");

	private volatile long requestTimeout = 10000;

	private volatile EvaluationContext evaluationContext = new StandardEvaluationContext();

	/**
	 * @param requestTimeout the requestTimeout to set
	 */
	public void setRequestTimeout(long requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	/**
	 * @param remoteTimeout the remoteTimeout to set
	 */
	public void setRemoteTimeout(long remoteTimeout) {
		this.remoteTimeoutExpression = new LiteralExpression("" + remoteTimeout);
	}

	/**
	 * @param remoteTimeoutExpression the remoteTimeoutExpression to set
	 */
	public void setRemoteTimeoutExpression(Expression remoteTimeoutExpression) {
		this.remoteTimeoutExpression = remoteTimeoutExpression;
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Assert.notNull(connectionFactory, this.getClass().getName() +
				" requires a client connection factory");
		boolean haveSemaphore = false;
		TcpConnection connection = null;
		String connectionId = null;
		try {
			if (!this.isSingleUse) {
				logger.debug("trying semaphore");
				if (!this.semaphore.tryAcquire(this.requestTimeout, TimeUnit.MILLISECONDS)) {
					throw new MessageTimeoutException(requestMessage, "Timed out waiting for connection");
				}
				haveSemaphore = true;
				if (logger.isDebugEnabled()) {
					logger.debug("got semaphore");
				}
			}
			connection = this.connectionFactory.getConnection();
			AsyncReply reply = new AsyncReply(this.remoteTimeoutExpression.getValue(this.evaluationContext,
					requestMessage, Long.class));
			connectionId = connection.getConnectionId();
			pendingReplies.put(connectionId, reply);
			if (logger.isDebugEnabled()) {
				logger.debug("Added pending reply " + connectionId);
			}
			connection.send(requestMessage);
			Message<?> replyMessage = reply.getReply();
			if (replyMessage == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Remote Timeout on " + connectionId);
				}
				// The connection is dirty - force it closed.
				this.connectionFactory.forceClose(connection);
				throw new MessageTimeoutException(requestMessage, "Timed out waiting for response");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Response " + replyMessage);
			}
			return replyMessage;
		}
		catch (Exception e) {
			logger.error("Tcp Gateway exception", e);
			if (e instanceof MessagingException) {
				throw (MessagingException) e;
			}
			throw new MessagingException("Failed to send or receive", e);
		}
		finally {
			if (connectionId != null) {
				pendingReplies.remove(connectionId);
				if (logger.isDebugEnabled()) {
					logger.debug("Removed pending reply " + connectionId);
				}
				if (this.isSingleUse) {
					connection.close();
				}
			}
			if (haveSemaphore) {
				this.semaphore.release();
				if (logger.isDebugEnabled()) {
					logger.debug("released semaphore");
				}
			}
		}
	}

	@Override
	public boolean onMessage(Message<?> message) {
		String connectionId = (String) message.getHeaders().get(IpHeaders.CONNECTION_ID);
		if (connectionId == null) {
			logger.error("Cannot correlate response - no connection id");
			publishNoConnectionEvent(message, null, "Cannot correlate response - no connection id");
			return false;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("onMessage: " + connectionId + "(" + message + ")");
		}
		AsyncReply reply = pendingReplies.get(connectionId);
		if (reply == null) {
			if (message instanceof ErrorMessage) {
				/*
				 * Socket errors are sent here so they can be conveyed to any waiting thread.
				 * If there's not one, simply ignore.
				 */
				return false;
			}
			else {
				String errorMessage = "Cannot correlate response - no pending reply for " + connectionId;
				logger.error(errorMessage);
				publishNoConnectionEvent(message, connectionId, errorMessage);
				return false;
			}
		}
		reply.setReply(message);
		return false;
	}

	private void publishNoConnectionEvent(Message<?> message, String connectionId, String errorMessage) {
		ApplicationEventPublisher applicationEventPublisher = this.connectionFactory.getApplicationEventPublisher();
		if (applicationEventPublisher != null) {
			applicationEventPublisher.publishEvent(new TcpConnectionFailedCorrelationEvent(this, connectionId,
					new MessagingException(message, errorMessage)));
		}
	}

	public void setConnectionFactory(AbstractClientConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
		connectionFactory.registerListener(this);
		connectionFactory.registerSender(this);
		this.isSingleUse = connectionFactory.isSingleUse();
	}

	@Override
	public void addNewConnection(TcpConnection connection) {
		// do nothing - no asynchronous multiplexing supported
	}

	@Override
	public void removeDeadConnection(TcpConnection connection) {
		// do nothing - no asynchronous multiplexing supported
	}

	/**
	 * Specify the Spring Integration reply channel. If this property is not
	 * set the gateway will check for a 'replyChannel' header on the request.
	 *
	 * @param replyChannel The reply channel.
	 */
	public void setReplyChannel(MessageChannel replyChannel) {
		this.setOutputChannel(replyChannel);
	}
	@Override
	public String getComponentType(){
		return "ip:tcp-outbound-gateway";
	}

	@Override
	public void start() {
		this.connectionFactory.start();
	}

	@Override
	public void stop() {
		this.connectionFactory.stop();
	}

	@Override
	public boolean isRunning() {
		return this.connectionFactory.isRunning();
	}

	/**
	 * @return the connectionFactory
	 */
	protected AbstractConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	/**
	 * Class used to coordinate the asynchronous reply to its request.
	 *
	 * @author Gary Russell
	 * @since 2.0
	 */
	private class AsyncReply {

		private final CountDownLatch latch;

		private final CountDownLatch secondChanceLatch;

		private final long remoteTimeout;

		private volatile Message<?> reply;

		public AsyncReply(long remoteTimeout) {
			this.latch = new CountDownLatch(1);
			this.secondChanceLatch = new CountDownLatch(1);
			this.remoteTimeout = remoteTimeout;
		}

		/**
		 * Sender blocks here until the reply is received, or we time out
		 * @return The return message or null if we time out
		 * @throws Exception
		 */
		public Message<?> getReply() throws Exception {
			try {
				if (!this.latch.await(this.remoteTimeout, TimeUnit.MILLISECONDS)) {
					return null;
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			boolean waitForMessageAfterError = true;
			while (reply instanceof ErrorMessage) {
				if (waitForMessageAfterError) {
					/*
					 * Possible race condition with NIO; we might have received the close
					 * before the reply, on a different thread.
					 */
					logger.debug("second chance");
					this.secondChanceLatch.await(2, TimeUnit.SECONDS);
					waitForMessageAfterError = false;
				}
				else if (reply.getPayload() instanceof MessagingException) {
					throw (MessagingException) reply.getPayload();
				}
				else {
					throw new MessagingException("Exception while awaiting reply", (Throwable) reply.getPayload());
				}
			}
			return this.reply;
		}

		/**
		 * We have a race condition when a socket is closed right after the reply is received. The close "error"
		 * might arrive before the actual reply. Overwrite an error with a good reply, but not vice-versa.
		 * @param reply the reply message.
		 */
		public void setReply(Message<?> reply) {
			if (this.reply == null) {
				this.reply = reply;
				this.latch.countDown();
			}
			else if (this.reply instanceof ErrorMessage) {
				this.reply = reply;
				this.secondChanceLatch.countDown();
			}
		}
	}

}
