/*
 * Copyright 2001-2014 the original author or authors.
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

import org.springframework.context.SmartLifecycle;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
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
public class TcpOutboundGateway extends AbstractReplyProducingMessageHandler implements TcpSender, TcpListener, SmartLifecycle {

	private volatile AbstractClientConnectionFactory connectionFactory;

	private final Map<String, AsyncReply> pendingReplies = new ConcurrentHashMap<String, AsyncReply>();

	private final Semaphore semaphore = new Semaphore(1, true);

	private volatile long remoteTimeout = 10000L;

	private volatile boolean remoteTimeoutSet = false;

	private volatile long requestTimeout = 10000;

	private volatile boolean autoStartup = true;

	private volatile int phase;

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
		this.remoteTimeout = remoteTimeout;
		this.remoteTimeoutSet = true;
	}

	@Override
	public void setSendTimeout(long sendTimeout) {
		super.setSendTimeout(sendTimeout);
		/*
		 * For backwards compatibility, also set the remote
		 * timeout to this value, unless it has been
		 * explicitly set.
		 */
		if (!this.remoteTimeoutSet) {
			this.remoteTimeout = sendTimeout;
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Assert.notNull(connectionFactory, this.getClass().getName() +
				" requires a client connection factory");
		boolean haveSemaphore = false;
		String connectionId = null;
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
			connectionId = connection.getConnectionId();
			pendingReplies.put(connectionId, reply);
			if (logger.isDebugEnabled()) {
				logger.debug("Added " + connection.getConnectionId());
			}
			connection.send(requestMessage);
			Message<?> replyMessage = reply.getReply();
			if (replyMessage == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Remote Timeout on " + connection.getConnectionId());
				}
				// The connection is dirty - force it closed.
				this.connectionFactory.forceClose(connection);
				throw new MessageTimeoutException(requestMessage, "Timed out waiting for response");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Respose " + replyMessage);
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
				logger.error("Cannot correlate response - no pending reply");
				return false;
			}
		}
		reply.setReply(message);
		return false;
	}

	public void setConnectionFactory(AbstractConnectionFactory connectionFactory) {
		// TODO: In 3.0 Change parameter type to AbstractClientConnectionFactory
		Assert.isTrue(connectionFactory instanceof AbstractClientConnectionFactory,
				this.getClass().getName() + " requires a client connection factory");
		this.connectionFactory = (AbstractClientConnectionFactory) connectionFactory;
		connectionFactory.registerListener(this);
		connectionFactory.registerSender(this);
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

	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void stop(Runnable callback) {
		this.connectionFactory.stop(callback);
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
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

		private volatile Message<?> reply;

		public AsyncReply() {
			this.latch = new CountDownLatch(1);
			this.secondChanceLatch = new CountDownLatch(1);
		}

		/**
		 * Sender blocks here until the reply is received, or we time out
		 * @return The return message or null if we time out
		 * @throws Exception
		 */
		public Message<?> getReply() throws Exception {
			try {
				if (!this.latch.await(remoteTimeout, TimeUnit.MILLISECONDS)) {
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
		 * @param reply
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
