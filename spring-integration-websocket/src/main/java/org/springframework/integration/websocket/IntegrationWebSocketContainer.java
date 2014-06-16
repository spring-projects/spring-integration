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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * The high-level 'connection factory pattern' contract over low-level Web-Socket
 * configuration.
 * <p>
 * Provides the composition for the internal {@link WebSocketHandler}
 * implementation, which is used with native Web-Socket containers.
 * <p>
 * Collects established {@link WebSocketSession}s, which can be accessed using
 * {@link #getSession(String)}.
 * <p>
 * Can accept the {@link WebSocketListener} to delegate {@link WebSocketSession} events
 * from the internal {@link IntegrationWebSocketContainer.IntegrationWebSocketHandler}.
 * <p>
 * Supported sub-protocols can be configured, but {@link WebSocketListener#getSubProtocols()}
 * have a precedent.
 *
 * @author Artem Bilan
 * @since 4.1
 * @see org.springframework.integration.websocket.inbound.WebSocketInboundChannelAdapter
 * @see org.springframework.integration.websocket.outbound.WebSocketOutboundMessageHandler
 */
public abstract class IntegrationWebSocketContainer implements ApplicationEventPublisherAware, DisposableBean {

	protected final Log logger = LogFactory.getLog(this.getClass());

	protected final WebSocketHandler webSocketHandler = new IntegrationWebSocketHandler();

	protected final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<String, WebSocketSession>();

	private final List<String> supportedProtocols = new ArrayList<String>();

	private volatile WebSocketListener messageListener;

	private volatile int sendTimeLimit = 10 * 1000;

	private volatile int sendBufferSizeLimit = 512 * 1024;

	private ApplicationEventPublisher eventPublisher;

	public void setSendTimeLimit(int sendTimeLimit) {
		this.sendTimeLimit = sendTimeLimit;
	}

	public void setSendBufferSizeLimit(int sendBufferSizeLimit) {
		this.sendBufferSizeLimit = sendBufferSizeLimit;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}

	public void setMessageListener(WebSocketListener messageListener) {
		Assert.state(this.messageListener == null || this.messageListener == messageListener,
				"'messageListener' is already configured");
		this.messageListener = messageListener;
	}

	public void setSupportedProtocols(String... protocols) {
		this.supportedProtocols.clear();
		addSupportedProtocols(protocols);
	}

	public void addSupportedProtocols(String... protocols) {
		for (String protocol : protocols) {
			this.supportedProtocols.add(protocol.toLowerCase());
		}
	}

	public List<String> getSubProtocols() {
		List<String> protocols = new ArrayList<String>();
		if (this.messageListener != null) {
			protocols.addAll(this.messageListener.getSubProtocols());
		}
		protocols.addAll(this.supportedProtocols);
		return Collections.unmodifiableList(protocols);
	}

	public WebSocketSession getSession(String sessionId) throws Exception {
		WebSocketSession session = this.sessions.get(sessionId);
		Assert.notNull(session, "Session not found for id '" + sessionId + "'");
		return session;
	}

	public void closeSession(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		// Session may be unresponsive so clear first
		session.close(closeStatus);
		this.webSocketHandler.afterConnectionClosed(session, closeStatus);
	}

	@Override
	public void destroy() throws Exception {
		// Notify sessions to stop flushing messages
		for (WebSocketSession session : this.sessions.values()) {
			try {
				session.close(CloseStatus.GOING_AWAY);
			}
			catch (Throwable t) {
				logger.error("Failed to close session id '" + session.getId() + "': " + t.getMessage());
			}
		}
		this.sessions.clear();
	}

	private void publishEvent(ApplicationEvent event) {
		try {
			this.eventPublisher.publishEvent(event);
		}
		catch (Throwable ex) {
			logger.error("Error while publishing " + event, ex);
		}
	}

	/**
	 * An internal {@link WebSocketHandler} implementation to be used with native
	 * Web-Socket containers.
	 * <p>
	 * Delegates all operations to the wrapping {@link IntegrationWebSocketContainer}
	 * and its {@link WebSocketListener}.
	 */
	private class IntegrationWebSocketHandler implements WebSocketHandler, SubProtocolCapable {

		@Override
		public List<String> getSubProtocols() {
			return IntegrationWebSocketContainer.this.getSubProtocols();
		}

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			session = new ConcurrentWebSocketSessionDecorator(session,
					IntegrationWebSocketContainer.this.sendTimeLimit,
					IntegrationWebSocketContainer.this.sendBufferSizeLimit);

			IntegrationWebSocketContainer.this.sessions.put(session.getId(), session);
			if (logger.isDebugEnabled()) {
				logger.debug("Started WebSocket session = " + session.getId() + ", number of sessions = "
						+ IntegrationWebSocketContainer.this.sessions.size());
			}
			if (IntegrationWebSocketContainer.this.messageListener != null) {
				IntegrationWebSocketContainer.this.messageListener.afterSessionStarted(session);
			}
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
			WebSocketSession removed = IntegrationWebSocketContainer.this.sessions.remove(session.getId());
			if (removed != null) {
				if (IntegrationWebSocketContainer.this.messageListener != null) {
					IntegrationWebSocketContainer.this.messageListener.afterSessionEnded(session, closeStatus);
				}
				else if (IntegrationWebSocketContainer.this.eventPublisher != null) {
					publishEvent(new SessionDisconnectEvent(this, session.getId(), closeStatus));
				}
			}
		}

		@Override
		public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
			WebSocketSession removed = IntegrationWebSocketContainer.this.sessions.remove(session.getId());
			if (removed != null) {
				IntegrationWebSocketContainer.this.sessions.remove(session.getId());
				if (IntegrationWebSocketContainer.this.eventPublisher != null) {
					publishEvent(new SessionErrorEvent(this, session.getId(), exception));
				}
			}
		}

		@Override
		public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
			if (IntegrationWebSocketContainer.this.messageListener != null) {
				IntegrationWebSocketContainer.this.messageListener.onMessage(session, message);
			}
			else if (logger.isInfoEnabled()) {
				logger.info("This 'WebSocketHandlerContainer' isn't configured with 'WebSocketMessageListener'."
						+ " Received messages are ignored. Current message is: " + message);
			}
		}

		@Override
		public boolean supportsPartialMessages() {
			return false;
		}

	}

}
