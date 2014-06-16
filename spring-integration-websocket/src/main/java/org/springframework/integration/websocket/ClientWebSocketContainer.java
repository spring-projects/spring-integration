/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.websocket;

import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.ConnectionManagerSupport;
import org.springframework.web.socket.client.WebSocketClient;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public final class ClientWebSocketContainer extends IntegrationWebSocketContainer implements SmartLifecycle {

	private final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

	private final ConnectionManagerSupport connectionManager;

	private WebSocketSession clientSession;


	public ClientWebSocketContainer(WebSocketClient client, String uriTemplate, Object... uriVariables) {
		Assert.notNull(client, "'client' must not be null");
		this.connectionManager = new IntegrationWebSocketConnectionManager(client, uriTemplate, uriVariables);
	}

	public void setOrigin(String origin) {
		this.headers.setOrigin(origin);
	}

	public void setHeaders(HttpHeaders headers) {
		this.headers.clear();
		this.headers.putAll(headers);
	}

	@Override
	public WebSocketSession getSession(String sessionId) {
		Assert.state(this.clientSession != null,
				"'clientSession' has not been established. Consider to 'start' this container.");
		return this.clientSession;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.connectionManager.setAutoStartup(autoStartup);
	}

	public void setPhase(int phase) {
		this.connectionManager.setPhase(phase);
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
	public void start() {
		this.connectionManager.start();
	}

	@Override
	public void stop() {
		this.connectionManager.stop();
	}

	@Override
	public void stop(Runnable callback) {
		this.connectionManager.stop(callback);
	}


	private class IntegrationWebSocketConnectionManager extends ConnectionManagerSupport {

		private final WebSocketClient client;

		private final boolean syncClientLifecycle;

		public IntegrationWebSocketConnectionManager(WebSocketClient client, String uriTemplate, Object... uriVariables) {
			super(uriTemplate, uriVariables);
			this.client = client;
			this.syncClientLifecycle = ((client instanceof SmartLifecycle) && !((SmartLifecycle) client).isRunning());
		}

		@Override
		public void startInternal() {
			if (this.syncClientLifecycle) {
				((SmartLifecycle) this.client).start();
			}
			super.startInternal();
		}

		@Override
		public void stopInternal() throws Exception {
			if (this.syncClientLifecycle) {
				((SmartLifecycle) this.client).stop();
			}
			try {
				super.stopInternal();
			}
			finally {
				ClientWebSocketContainer.this.clientSession = null;
			}
		}

		@Override
		protected void openConnection() {

			logger.info("Connecting to WebSocket at " + getUri());
			ClientWebSocketContainer.this.headers.setSecWebSocketProtocol(ClientWebSocketContainer.this.getSubProtocols());
			ListenableFuture<WebSocketSession> future =
					this.client.doHandshake(ClientWebSocketContainer.this.webSocketHandler,
							ClientWebSocketContainer.this.headers, getUri());

			future.addCallback(new ListenableFutureCallback<WebSocketSession>() {

				@Override
				public void onSuccess(WebSocketSession session) {
					ClientWebSocketContainer.this.clientSession = session;
					logger.info("Successfully connected");
				}

				@Override
				public void onFailure(Throwable t) {
					logger.error("Failed to connect", t);
				}
			});
		}

		@Override
		protected void closeConnection() throws Exception {
			if (ClientWebSocketContainer.this.clientSession != null) {
				ClientWebSocketContainer.this.closeSession(ClientWebSocketContainer.this.clientSession,
						CloseStatus.NORMAL);
			}
		}

		@Override
		protected boolean isConnected() {
			return ((ClientWebSocketContainer.this.clientSession != null)
					&& (ClientWebSocketContainer.this.clientSession.isOpen()));
		}

	}

}
