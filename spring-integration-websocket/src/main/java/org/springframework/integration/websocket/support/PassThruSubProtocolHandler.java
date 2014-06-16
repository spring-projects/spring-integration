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

package org.springframework.integration.websocket.support;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SubProtocolHandler;

/**
 * The simple 'pass thru' {@link SubProtocolHandler}, when there is no interests in the
 * WebSocket sub-protocols.
 * This class just convert {@link Message} to the {@link WebSocketMessage}
 * on 'send' part and vise versa - on 'receive' part.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class PassThruSubProtocolHandler implements SubProtocolHandler {

	final List<String> supportedProtocols = new ArrayList<String>();

	public void setSupportedProtocols(String... supportedProtocols) {
		Assert.noNullElements(supportedProtocols, "'supportedProtocols' must not be empty");
		this.supportedProtocols.addAll(Arrays.asList(supportedProtocols));
	}

	@Override
	public List<String> getSupportedProtocols() {
		return supportedProtocols;
	}

	@Override
	public void handleMessageFromClient(WebSocketSession session, WebSocketMessage<?> webSocketMessage,
			MessageChannel outputChannel) throws Exception {
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		headerAccessor.setSessionId(session.getId());
		headerAccessor.setSessionAttributes(session.getAttributes());
		headerAccessor.setUser(session.getPrincipal());
		headerAccessor.setHeader("content-length", webSocketMessage.getPayloadLength());
		headerAccessor.setLeaveMutable(true);
		Message<?> message =
				MessageBuilder.createMessage(webSocketMessage.getPayload(), headerAccessor.getMessageHeaders());
		try {
			SimpAttributesContextHolder.setAttributesFromMessage(message);
			outputChannel.send(message);
		}
		finally {
			SimpAttributesContextHolder.resetAttributes();
		}
	}

	@Override
	public void handleMessageToClient(WebSocketSession session, Message<?> message) throws Exception {
		Object payload = message.getPayload();
		if (payload instanceof String) {
			session.sendMessage(new TextMessage((String) payload));
		}
		else if (payload instanceof byte[]) {
			session.sendMessage(new TextMessage((byte[]) payload));
		}
		else if (payload instanceof ByteBuffer) {
			session.sendMessage(new TextMessage(((ByteBuffer) payload).array()));
		}
		else {
			throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass()
					+ ". Can be one of: " + Arrays.<Class<?>>asList(String.class, byte[].class, ByteBuffer.class));
		}
	}

	@Override
	public String resolveSessionId(Message<?> message) {
		return SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
	}

	@Override
	public void afterSessionStarted(WebSocketSession session, MessageChannel outputChannel) throws Exception {
		// Subclasses might implement this method
	}

	@Override
	public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus, MessageChannel outputChannel)
			throws Exception {
		// Subclasses might implement this method
	}

}
