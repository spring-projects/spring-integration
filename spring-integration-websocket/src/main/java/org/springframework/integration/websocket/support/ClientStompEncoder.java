/*
 * Copyright 2017-2019 the original author or authors.
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
