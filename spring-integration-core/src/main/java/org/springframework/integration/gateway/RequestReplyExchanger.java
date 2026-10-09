/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Interface for a request/reply Message exchange. This will be used as a default
 * by {@link GatewayProxyFactoryBean} if no 'service-interface' property has been provided.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @see AsyncRequestReplyExchanger
 */
@FunctionalInterface
public interface RequestReplyExchanger {

	Message<?> exchange(Message<?> request) throws MessagingException;

}
