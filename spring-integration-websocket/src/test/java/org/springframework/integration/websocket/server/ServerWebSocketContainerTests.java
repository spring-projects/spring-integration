/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.websocket.server;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.integration.websocket.ServerWebSocketContainer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ServerWebSocketContainer}.
 *
 * @author Marcin Nowicki
 *
 * @since 7.0.5
 */
public class ServerWebSocketContainerTests {

	@Test
	public void setAllowedOriginPatternsIsUsedInRegistration() {
		WebSocketHandlerRegistry registry = mock();
		WebSocketHandlerRegistration registration = mock(Answers.RETURNS_SELF);
		given(registry.addHandler(any(), any(String[].class))).willReturn(registration);

		ServerWebSocketContainer container = new ServerWebSocketContainer("/test")
			.setAllowedOriginPatterns("https://example.com");

		container.registerWebSocketHandlers(registry);

		verify(registration).setAllowedOriginPatterns("https://example.com");
		verify(registration, never()).setAllowedOrigins(any());
	}

	@Test
	public void setAllowedOriginsIsUsedWhenNoPatternsConfigured() {
		WebSocketHandlerRegistry registry = mock();
		WebSocketHandlerRegistration registration = mock(Answers.RETURNS_SELF);
		given(registry.addHandler(any(), any(String[].class))).willReturn(registration);

		ServerWebSocketContainer container = new ServerWebSocketContainer("/test")
			.setAllowedOrigins("https://example.com");

		container.registerWebSocketHandlers(registry);

		verify(registration).setAllowedOrigins("https://example.com");
		verify(registration, never()).setAllowedOriginPatterns(any());
	}

	@Test
	public void bothOriginsAndOriginPatternsAreUsedWhenBothConfigured() {
		WebSocketHandlerRegistry registry = mock();
		WebSocketHandlerRegistration registration = mock(Answers.RETURNS_SELF);
		given(registry.addHandler(any(), any(String[].class))).willReturn(registration);

		ServerWebSocketContainer container = new ServerWebSocketContainer("/test")
			.setAllowedOrigins("https://example.com")
			.setAllowedOriginPatterns("https://*.example.com");

		container.registerWebSocketHandlers(registry);

		verify(registration).setAllowedOriginPatterns("https://*.example.com");
		verify(registration).setAllowedOrigins("https://example.com");
	}

}
