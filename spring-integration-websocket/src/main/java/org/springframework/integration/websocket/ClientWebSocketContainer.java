/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.integration.websocket;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.ConnectionManagerSupport;
import org.springframework.web.socket.client.WebSocketClient;

/**
 * The {@link IntegrationWebSocketContainer} implementation for the {@code client}
 * Web-Socket connection.
 * <p>
 * Represent the composition over an internal {@link ConnectionManagerSupport}
 * implementation.
 * <p>
 * Accepts the {@link #clientSession} {@link WebSocketSession} on
 * {@link ClientWebSocketContainer.IntegrationWebSocketConnectionManager#openConnection()}
 * event, which can be accessed from this container using {@link #getSession(String)}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.1
 */
public final class ClientWebSocketContainer extends IntegrationWebSocketContainer implements SmartLifecycle {

	private static final int DEFAULT_CONNECTION_TIMEOUT = 10;

	private final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

	private final IntegrationWebSocketConnectionManager connectionManager;

	private volatile CountDownLatch connectionLatch;

	private volatile WebSocketSession clientSession;

	private volatile Throwable openConnectionException;

	private volatile int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

	private volatile boolean connecting;

	public ClientWebSocketContainer(WebSocketClient client, String uriTemplate, Object... uriVariables) {
		Assert.notNull(client, "'client' must not be null");
		this.connectionManager = new IntegrationWebSocketConnectionManager(client, uriTemplate, uriVariables);
	}

	public void setOrigin(String origin) {
		this.headers.setOrigin(origin);
	}

	public void setHeadersMap(Map<String, String> headers) {
		Assert.notNull(headers, "'headers' must not be null");
		HttpHeaders httpHeaders = new HttpHeaders();
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			String[] values = StringUtils.commaDelimitedListToStringArray(entry.getValue());
			for (String v : values) {
				httpHeaders.add(entry.getKey(), v);
			}
		}
		setHeaders(httpHeaders);
	}

	public void setHeaders(HttpHeaders headers) {
		this.headers.putAll(headers);
	}

	/**
	 * Set the connection timeout in seconds; default: 10.
	 * @param connectionTimeout the timeout in seconds.
	 * @since 4.2
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * Return the {@link #clientSession} {@link WebSocketSession}.
	 * Independently of provided argument, this method always returns only the
	 * established {@link #clientSession}
	 * @param sessionId the {@code sessionId}. Can be {@code null}.
	 * @return the {@link #clientSession}, if established.
	 */
	@Override
	public WebSocketSession getSession(String sessionId) {
		if (isRunning()) {
			if (!isConnected() && !this.connecting) {
				stop();
				start();
			}

			try {
				this.connectionLatch.await(this.connectionTimeout, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				logger.error("'clientSession' has not been established during 'openConnection'");
			}
			this.connecting = false;
		}

		try {
			if (this.openConnectionException != null) {
				throw new IllegalStateException(this.openConnectionException);
			}
			Assert.state(this.clientSession != null,
					"'clientSession' has not been established. Consider to 'start' this container.");
		}
		catch (IllegalStateException e) {
			stop();
			throw e;
		}
		return this.clientSession;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.connectionManager.setAutoStartup(autoStartup);
	}

	public void setPhase(int phase) {
		this.connectionManager.setPhase(phase);
	}

	/**
	 * Return {@code true} if the {@link #clientSession} is opened.
	 * @return the {@link WebSocketSession#isOpen()} state.
	 * @since 4.2.6
	 */
	public boolean isConnected() {
		return this.connectionManager.isConnected();
	}

	@Override
	public boolean isAutoStartup() {
		return this.connectionManager.isAutoStartup();
	}

	@Override
	public int getPhase() {
		return this.connectionManager.getPhase();
	}

	@Override
	public boolean isRunning() {
		return this.connectionManager.isRunning();
	}

	@Override
	public synchronized void start() {
		if (!isRunning()) {
			this.clientSession = null;
			this.openConnectionException = null;
			this.connectionLatch = new CountDownLatch(1);
			this.connectionManager.start();
		}
	}

	@Override
	public void stop() {
		this.connectionManager.stop();
	}

	@Override
	public void stop(Runnable callback) {
		this.connectionManager.stop(callback);
	}

	/**
	 * The {@link ConnectionManagerSupport} implementation to provide open/close operations
	 * for an external Web-Socket service, based on provided {@link WebSocketClient} and {@code uriTemplate}.
	 * <p>
	 * Opened {@link WebSocketSession} is populated to the wrapping {@link ClientWebSocketContainer}.
	 * <p>
	 * The {@link #webSocketHandler} is used to handle {@link WebSocketSession} events.
	 */
	private final class IntegrationWebSocketConnectionManager extends ConnectionManagerSupport {

		private final WebSocketClient client;

		private final boolean syncClientLifecycle;

		IntegrationWebSocketConnectionManager(WebSocketClient client, String uriTemplate,
				Object... uriVariables) {
			super(uriTemplate, uriVariables);
			this.client = client;
			this.syncClientLifecycle = ((client instanceof Lifecycle) && !((Lifecycle) client).isRunning());
		}

		@Override
		public void startInternal() {
			if (this.syncClientLifecycle) {
				((Lifecycle) this.client).start();
			}
			ClientWebSocketContainer.this.connecting = true;
			super.startInternal();
		}

		@Override
		public void stopInternal() throws Exception { // NOSONAR honor super
			if (this.syncClientLifecycle) {
				((Lifecycle) this.client).stop();
			}
			try {
				super.stopInternal();
			}
			finally {
				ClientWebSocketContainer.this.clientSession = null;
				ClientWebSocketContainer.this.openConnectionException = null;
			}
		}

		@Override
		protected void openConnection() {
			if (logger.isInfoEnabled()) {
				logger.info("Connecting to WebSocket at " + getUri());
			}
			ClientWebSocketContainer.this.headers.setSecWebSocketProtocol(getSubProtocols());
			ListenableFuture<WebSocketSession> future =
					this.client.doHandshake(ClientWebSocketContainer.this.webSocketHandler,
							ClientWebSocketContainer.this.headers, getUri());

			future.addCallback(new ListenableFutureCallback<WebSocketSession>() {

				@Override
				public void onSuccess(WebSocketSession session) {
					ClientWebSocketContainer.this.clientSession = session;
					logger.info("Successfully connected");
					ClientWebSocketContainer.this.connectionLatch.countDown();
				}

				@Override
				public void onFailure(Throwable t) {
					logger.error("Failed to connect", t);
					ClientWebSocketContainer.this.openConnectionException = t;
					ClientWebSocketContainer.this.connectionLatch.countDown();
				}

			});
		}

		@Override
		protected void closeConnection() throws Exception { // NOSONAR
			if (ClientWebSocketContainer.this.clientSession != null) {
				closeSession(ClientWebSocketContainer.this.clientSession, CloseStatus.NORMAL);
			}
		}

		@Override
		protected boolean isConnected() {
			return ((ClientWebSocketContainer.this.clientSession != null)
					&& (ClientWebSocketContainer.this.clientSession.isOpen()));
		}

	}

}
