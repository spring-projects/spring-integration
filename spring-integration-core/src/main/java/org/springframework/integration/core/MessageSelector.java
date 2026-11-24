/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.core;

import org.springframework.messaging.Message;

/**
 * Strategy interface for message selection.
 *
 * @author Mark Fisher
 */
@FunctionalInterface
public interface MessageSelector extends GenericSelector<Message<?>> {

	boolean accept(Message<?> message);

}
