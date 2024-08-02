/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.websocket.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SubProtocolHandler;

/**
 * The utility class to encapsulate search algorithms for a set of provided {@link SubProtocolHandler}s.
 * <p>
 * For internal use only.
 *
 * @author Andy Wilkinson
 * @author Artem Bilan
 *
 * @since 4.1
 *
 * @see org.springframework.integration.websocket.inbound.WebSocketInboundChannelAdapter
 * @see org.springframework.integration.websocket.outbound.WebSocketOutboundMessageHandler
 */
public final class SubProtocolHandlerRegistry {

	private static final Log LOGGER = LogFactory.getLog(SubProtocolHandlerRegistry.class);

	private final Map<String, SubProtocolHandler> protocolHandlers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	private final SubProtocolHandler defaultProtocolHandler;

	public SubProtocolHandlerRegistry(List<SubProtocolHandler> protocolHandlers) {
		this(protocolHandlers, null);
	}

	public SubProtocolHandlerRegistry(SubProtocolHandler defaultProtocolHandler) {
		this(null, defaultProtocolHandler);
	}

	public SubProtocolHandlerRegistry(List<SubProtocolHandler> protocolHandlers,
			SubProtocolHandler defaultProtocolHandler) {

		Assert.state(!CollectionUtils.isEmpty(protocolHandlers) || defaultProtocolHandler != null,
				"One of 'protocolHandlers' or 'defaultProtocolHandler' must be provided");

		configureProtocolHandlers(protocolHandlers);

		if (this.protocolHandlers.size() == 1 && defaultProtocolHandler == null) {
			this.defaultProtocolHandler = this.protocolHandlers.values().iterator().next();
		}
		else {
			this.defaultProtocolHandler = defaultProtocolHandler;
			if (this.protocolHandlers.isEmpty() && this.defaultProtocolHandler != null) {
				List<String> protocols = this.defaultProtocolHandler.getSupportedProtocols();
				populateProtocolsForHandler(this.defaultProtocolHandler, protocols);
			}
		}
	}

	private void configureProtocolHandlers(List<SubProtocolHandler> protocolHandlers) {
		if (!CollectionUtils.isEmpty(protocolHandlers)) {
			for (SubProtocolHandler handler : protocolHandlers) {
				List<String> protocols = handler.getSupportedProtocols();
				if (CollectionUtils.isEmpty(protocols)) {
					if (LOGGER.isWarnEnabled()) {
						LOGGER.warn("No sub-protocols, ignoring handler " + handler);
					}
					continue;
				}
				populateProtocolsForHandler(handler, protocols);
			}
		}
	}

	private void populateProtocolsForHandler(SubProtocolHandler handler, List<String> protocols) {
		for (String protocol : protocols) {
			SubProtocolHandler replaced = this.protocolHandlers.put(protocol, handler);
			if (replaced != null) {
				throw new IllegalStateException("Failed to map handler " + handler
						+ " to protocol '" + protocol + "', it is already mapped to handler " + replaced);
			}
		}
	}

	/**
	 * Resolves the {@link SubProtocolHandler} for the given {@code session} using
	 * its {@link WebSocketSession#getAcceptedProtocol() accepted sub-protocol}.
	 * @param session The session to resolve the sub-protocol handler for
	 * @return The sub-protocol handler
	 * @throws IllegalStateException if a protocol handler cannot be resolved
	 */
	public SubProtocolHandler findProtocolHandler(WebSocketSession session) {
		SubProtocolHandler handler;
		String protocol = session.getAcceptedProtocol();
		if (StringUtils.hasText(protocol)) {
			handler = this.protocolHandlers.get(protocol);
			Assert.state(handler != null,
					() -> "No handler for sub-protocol '" + protocol + "', handlers = " + this.protocolHandlers);
		}
		else {
			handler = this.defaultProtocolHandler;
			Assert.state(handler != null,
					"No sub-protocol was requested and a default sub-protocol handler was not configured");
		}
		return handler;
	}

	/**
	 * Resolves the {@code sessionId} for the given {@code message} using
	 * the {@link SubProtocolHandler#resolveSessionId} algorithm.
	 * @param message The message to resolve the {@code sessionId} from.
	 * @return The sessionId or {@code null}, if no one {@link SubProtocolHandler}
	 * can't resolve it against provided {@code message}.
	 */
	public String resolveSessionId(Message<?> message) {
		for (SubProtocolHandler handler : this.protocolHandlers.values()) {
			String sessionId = handler.resolveSessionId(message);
			if (sessionId != null) {
				return sessionId;
			}
		}
		if (this.defaultProtocolHandler != null) {
			String sessionId = this.defaultProtocolHandler.resolveSessionId(message);
			if (sessionId != null) {
				return sessionId;
			}
		}
		return null;
	}

	/**
	 * Return the {@link List} of sub-protocols from provided {@link SubProtocolHandler}.
	 * @return The {@link List} of supported sub-protocols.
	 */
	public List<String> getSubProtocols() {
		return new ArrayList<>(this.protocolHandlers.keySet());
	}

}
