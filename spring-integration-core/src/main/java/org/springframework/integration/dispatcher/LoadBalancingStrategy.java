/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.dispatcher;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * Strategy for determining the iteration order of a MessageHandler list.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 1.0.3
 */
@FunctionalInterface
public interface LoadBalancingStrategy {

	Iterator<MessageHandler> getHandlerIterator(Message<?> message, Collection<MessageHandler> handlers);

}
