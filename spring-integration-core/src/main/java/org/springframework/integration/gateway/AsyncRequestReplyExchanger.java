/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
