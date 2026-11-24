/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.redis.outbound;

import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 * @since 4.0
 */
@FunctionalInterface
public interface ArgumentsStrategy {

	Object[] resolve(String command, Message<?> message);

}
