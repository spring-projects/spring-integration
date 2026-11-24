/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.support;

import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
public interface MessageBuilderFactory {

	<T> AbstractIntegrationMessageBuilder<T> fromMessage(Message<T> message);

	<T> AbstractIntegrationMessageBuilder<T> withPayload(T payload);

}
