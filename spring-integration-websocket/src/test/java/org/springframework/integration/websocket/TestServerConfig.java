/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
import org.springframework.messaging.support.ChannelInterceptor;
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
 * @author Gary Russell
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
		directChannel.addInterceptor(new ChannelInterceptor() {

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

