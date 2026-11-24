/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
