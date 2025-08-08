/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.websocket.support;

import java.util.Map;

import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

/**
 * A {@link StompEncoder} extension to prepare a message for sending/receiving
 * before/after encoding/decoding when used from WebSockets client side.
 * For example it updates the {@code stompCommand} header from the {@code MESSAGE}
 * to {@code SEND} frame, which is the case of
 * {@link org.springframework.web.socket.messaging.StompSubProtocolHandler}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.3.13
 */
public class ClientStompEncoder extends StompEncoder {

	@Override
	public byte[] encode(Map<String, Object> headers, byte[] payload) {
		if (StompCommand.MESSAGE.equals(headers.get("stompCommand"))) {
			StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.create(StompCommand.SEND);
			stompHeaderAccessor.copyHeadersIfAbsent(headers);
			return super.encode(stompHeaderAccessor.getMessageHeaders(), payload);
		}
		else {
			return super.encode(headers, payload);
		}
	}

}
