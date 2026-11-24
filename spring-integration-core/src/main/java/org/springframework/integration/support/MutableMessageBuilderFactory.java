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
public class MutableMessageBuilderFactory implements MessageBuilderFactory {

	@Override
	public <T> MutableMessageBuilder<T> fromMessage(Message<T> message) {
		return MutableMessageBuilder.fromMessage(message);
	}

	@Override
	public <T> MutableMessageBuilder<T> withPayload(T payload) {
		return MutableMessageBuilder.withPayload(payload);
	}

}
