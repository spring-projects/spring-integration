/*
 * Copyright 2001-2020 the original author or authors.
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

package org.springframework.integration.ip.tcp;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.Lifecycle;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpConnectionFailedCorrelationEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpListener;
import org.springframework.integration.ip.tcp.connection.TcpNioConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpSender;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * TCP outbound gateway that uses a client connection factory. If the factory is configured
 * for single-use connections, each request is sent on a new connection; if the factory does not use
 * single use connections, each request is blocked until the previous response is received
 * (or times out). Asynchronous requests/responses over the same connection are not
 * supported - use a pair of outbound/inbound adapters for that use case.
 * <p>
 * {@link Lifecycle} methods delegate to the underlying {@link AbstractConnectionFactory}
 *
 *
 * @author Gary Russell
 *
 * @since 2.0
 */
public class TcpOutboundGateway extends AbstractReplyProducingMessageHandler
		implements TcpSender, TcpListener, Lifecycle {

	private static final long DEFAULT_REMOTE_TIMEOUT = 10_000L;

	private static final int DEFAULT_SECOND_CHANCE_DELAY = 2;

	private final Map<String, AsyncReply> pendingReplies = new ConcurrentHashMap<>();

	private final Semaphore semaphore = new Semaphore(1, true);

	private AbstractClientConnectionFactory connectionFactory;

	private boolean isSingleUse;

	private Expression remoteTimeoutExpression = new ValueExpression<>(DEFAULT_REMOTE_TIMEOUT);

	private long requestTimeout = 10000;

	private EvaluationContext evaluationContext = new StandardEvaluationContext();

	private boolean evaluationContextSet;

	private int secondChanceDelay = DEFAULT_SECOND_CHANCE_DELAY;

	private boolean closeStreamAfterSend;

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

	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		Assert.notNull(evaluationContext, "'evaluationContext' cannot be null");
		this.evaluationContext = evaluationContext;
		this.evaluationContextSet = true;
	}

	@Override
	protected void doInit() {
		super.doInit();
		if (!this.evaluationContextSet) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		}
		Assert.state(!this.closeStreamAfterSend || this.isSingleUse,
				"Single use connection needed with closeStreamAfterSend");
		if (isAsync()) {
			try {
				TcpConnectionSupport connection = this.connectionFactory.getConnection();
				if (connection instanceof TcpNioConnectionSupport) {
					setAsync(false);
					this.logger.warn("Async replies are not supported with NIO; see the reference manual");
				}
				if (this.isSingleUse) {
					connection.close();
				}
			}
			catch (Exception e) {
				this.logger.error("Could not check if async is supported", e);
			}
		}
	}

	/**
	 * When using NIO and the server closes the socket after sending the reply,
	 * an error message representing the close may appear before the reply.
	 * Set the delay, in seconds, to wait for an actual reply after an {@link ErrorMessage} is
	 * received. Default 2 seconds.
	 * @param secondChanceDelay the delay.
	 * @since 5.0.8
	 */
	public void setSecondChanceDelay(int secondChanceDelay) {
		this.secondChanceDelay = secondChanceDelay;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Assert.notNull(this.connectionFactory, this.getClass().getName() +
				" requires a client connection factory");
		boolean haveSemaphore = false;
		TcpConnection connection = null;
		String connectionId = null;
		boolean async = isAsync();
		try {
			haveSemaphore = acquireSemaphoreIfNeeded(requestMessage);
			connection = this.connectionFactory.getConnection();
			Long remoteTimeout = getRemoteTimeout(requestMessage);
			AsyncReply reply = new AsyncReply(remoteTimeout, connection, haveSemaphore, requestMessage, async);
			connectionId = connection.getConnectionId();
			this.pendingReplies.put(connectionId, reply);
			if (logger.isDebugEnabled()) {
				logger.debug("Added pending reply " + connectionId);
			}
			connection.send(requestMessage);
			if (this.closeStreamAfterSend) {
				connection.shutdownOutput();
			}
			if (async) {
				return reply.getFuture();
			}
			else {
				return getReply(requestMessage, connection, connectionId, reply);
			}
		}
		catch (RuntimeException | IOException e) {
			logger.error("Tcp Gateway exception", e);
			if (e instanceof MessagingException) {
				throw (MessagingException) e;
			}
			throw new MessagingException("Failed to send or receive", e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessageHandlingException(requestMessage, "Interrupted in the [" + this + ']', e);
		}
		finally {
			if (!async) {
				cleanUp(haveSemaphore, connection, connectionId);
			}
		}
	}

	private boolean acquireSemaphoreIfNeeded(Message<?> requestMessage) throws InterruptedException {
		if (!this.isSingleUse) {
			logger.debug("trying semaphore");
			if (!this.semaphore.tryAcquire(this.requestTimeout, TimeUnit.MILLISECONDS)) {
				throw new MessageTimeoutException(requestMessage, "Timed out waiting for connection");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("got semaphore");
			}
			return true;
		}
		return false;
	}

	private Long getRemoteTimeout(Message<?> requestMessage) {
		Long remoteTimeout = this.remoteTimeoutExpression.getValue(this.evaluationContext, requestMessage,
				Long.class);
		if (remoteTimeout == null) {
			remoteTimeout = DEFAULT_REMOTE_TIMEOUT;
			if (logger.isWarnEnabled()) {
				logger.warn("remoteTimeoutExpression evaluated to null; falling back to default for message "
						+ requestMessage);
			}
		}
		return remoteTimeout;
	}

	private Message<?> getReply(Message<?> requestMessage, TcpConnection connection, String connectionId,
			AsyncReply reply) {

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

	private void cleanUp(boolean haveSemaphore, TcpConnection connection, String connectionId) {
		if (connectionId != null) {
			this.pendingReplies.remove(connectionId);
			if (logger.isDebugEnabled()) {
				logger.debug("Removed pending reply " + connectionId);
			}
			if (this.isSingleUse) {
				connection.close();
			}
		}
		if (haveSemaphore) {
			this.semaphore.release();
			logger.debug("released semaphore");
		}
	}

	@Override
	public boolean onMessage(Message<?> message) {
		String connectionId = message.getHeaders().get(IpHeaders.CONNECTION_ID, String.class);
		if (connectionId == null) {
			logger.error("Cannot correlate response - no connection id");
			publishNoConnectionEvent(message, null, "Cannot correlate response - no connection id");
			return false;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("onMessage: " + connectionId + "(" + message + ")");
		}
		AsyncReply reply = this.pendingReplies.get(connectionId);
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
		if (isAsync()) {
			reply.getFuture().set(message);
			cleanUp(reply.isHaveSemaphore(), reply.getConnection(), connectionId);
		}
		else {
			reply.setReply(message);
		}
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
	 * @param replyChannel The reply channel.
	 */
	public void setReplyChannel(MessageChannel replyChannel) {
		this.setOutputChannel(replyChannel);
	}

	/**
	 * Specify the Spring Integration reply channel name. If this property is not
	 * set the gateway will check for a 'replyChannel' header on the request.
	 * @param replyChannel The reply channel.
	 * @since 5.0
	 */
	public void setReplyChannelName(String replyChannel) {
		this.setOutputChannelName(replyChannel);
	}

	/**
	 * Set to true to close the connection ouput stream after sending without
	 * closing the connection. Use to signal EOF to the server, such as when using
	 * a {@link org.springframework.integration.ip.tcp.serializer.ByteArrayRawSerializer}.
	 * Requires a single-use connection factory.
	 * @param closeStreamAfterSend true to close.
	 * @since 5.2
	 */
	public void setCloseStreamAfterSend(boolean closeStreamAfterSend) {
		this.closeStreamAfterSend = closeStreamAfterSend;
	}

	@Override
	public String getComponentType() {
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
		return this.connectionFactory;
	}

	/**
	 * Class used to coordinate the asynchronous reply to its request.
	 *
	 * @author Gary Russell
	 * @since 2.0
	 */
	private final class AsyncReply {

		private final CountDownLatch latch;

		private final CountDownLatch secondChanceLatch;

		private final long remoteTimeout;

		private final TcpConnection connection;

		private final boolean haveSemaphore;

		private final SettableListenableFuture<Message<?>> future = new SettableListenableFuture<>();

		private volatile Message<?> reply;

		AsyncReply(long remoteTimeout, TcpConnection connection, boolean haveSemaphore, Message<?> requestMessage,
				boolean async) {

			this.latch = new CountDownLatch(1);
			this.secondChanceLatch = new CountDownLatch(1);
			this.remoteTimeout = remoteTimeout;
			this.connection = connection;
			this.haveSemaphore = haveSemaphore;
			if (async && remoteTimeout > 0) {
				getTaskScheduler().schedule(() -> {
					TcpOutboundGateway.this.pendingReplies.remove(connection.getConnectionId());
					this.future.setException(
							new MessageTimeoutException(requestMessage, "Timed out waiting for response"));
				}, new Date(System.currentTimeMillis() + remoteTimeout));
			}
		}

		TcpConnection getConnection() {
			return this.connection;
		}

		boolean isHaveSemaphore() {
			return this.haveSemaphore;
		}

		/**
		 * Sender blocks here until the reply is received, or we time out
		 * @return The return message or null if we time out
		 */
		Message<?> getReply() {
			try {
				if (!this.latch.await(this.remoteTimeout, TimeUnit.MILLISECONDS)) {
					return null;
				}
			}
			catch (@SuppressWarnings("unused") InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			boolean waitForMessageAfterError = true;
			while (this.reply instanceof ErrorMessage) {
				if (waitForMessageAfterError) {
					/*
					 * Possible race condition with NIO; we might have received the close
					 * before the reply, on a different thread.
					 */
					logger.debug("second chance");
					try {
						this.secondChanceLatch
								.await(TcpOutboundGateway.this.secondChanceDelay, TimeUnit.SECONDS); // NOSONAR
					}
					catch (@SuppressWarnings("unused") InterruptedException e) {
						Thread.currentThread().interrupt();
						doThrowErrorMessagePayload();
					}
					waitForMessageAfterError = false;
				}
				else {
					doThrowErrorMessagePayload();
				}
			}
			return this.reply;
		}

		SettableListenableFuture<Message<?>> getFuture() {
			return this.future;
		}

		private void doThrowErrorMessagePayload() {
			if (this.reply.getPayload() instanceof MessagingException) {
				throw (MessagingException) this.reply.getPayload();
			}
			else {
				throw new MessagingException("Exception while awaiting reply", (Throwable) this.reply.getPayload());
			}
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
