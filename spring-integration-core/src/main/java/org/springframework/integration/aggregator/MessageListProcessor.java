/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.aggregator;

import java.util.Collection;

import org.springframework.messaging.Message;

/**
 * @author Dave Syer
 *
 */
@FunctionalInterface
public interface MessageListProcessor {

	Object process(Collection<? extends Message<?>> messages);

}
