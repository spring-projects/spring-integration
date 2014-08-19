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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

/**
 * @author Artem Bilan
 * @since 4.1
 */
@Configuration
@EnableWebSocket
/*
 * According to the WebSocketIntegrationConfigurationInitializer with usage of @EnableIntegration there is no need to
 * use @EnableWebSocket anymore. They are left here both to check consistency of registration algorithm.
 */
@EnableIntegration
public class TestServerConfig implements WebSocketConfigurer {

	@Bean
	public MessageChannel clientInboundChannel() {
		return new QueueChannel();
	}

	@Bean
	public AbstractSubscribableChannel clientOutboundChannel() {
		DirectChannel directChannel = new DirectChannel();
		directChannel.addInterceptor(new ChannelInterceptorAdapter() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
				headers.setLeaveMutable(true);
				return MessageBuilder.createMessage(message.getPayload(), headers.getMessageHeaders());
			}
		});
		return directChannel;
	}

	@Bean
	public SubProtocolHandler stompSubProtocolHandler() {
		return new StompSubProtocolHandler();
	}

	@Bean
	public WebSocketHandler subProtocolWebSocketHandler() {
		SubProtocolWebSocketHandler webSocketHandler =
				new SubProtocolWebSocketHandler(clientInboundChannel(), clientOutboundChannel());
		webSocketHandler.setDefaultProtocolHandler(stompSubProtocolHandler());
		return webSocketHandler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(subProtocolWebSocketHandler(), "/ws")
				.withSockJS();
	}

}

