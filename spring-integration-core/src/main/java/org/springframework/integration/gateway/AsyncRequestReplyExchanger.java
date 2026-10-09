/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.gateway;

import java.util.concurrent.CompletableFuture;

import org.springframework.messaging.Message;

/**
 * Messaging gateway contract for async request/reply Message exchange.
 *
 * @author Artem Bilan
 *
 * @since 6.5
 *
 * @see RequestReplyExchanger
 */
@FunctionalInterface
public interface AsyncRequestReplyExchanger {

	CompletableFuture<Message<?>> exchange(Message<?> request);

}
