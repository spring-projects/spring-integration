/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.transformer;

import org.springframework.messaging.Message;

/**
 * A base class for {@link Transformer} implementations that modify the payload
 * of a {@link Message}. If the return value is itself a Message, it will be
 * used as the result. Otherwise, the return value will be used as the payload
 * of the result Message.
 *
 * @param <T> inbound payload type.
 * @param <U> outbound payload type.
 *
 * @author Mark Fisher
 */
public abstract class AbstractPayloadTransformer<T, U> extends AbstractTransformer {

	@Override
	@SuppressWarnings("unchecked")
	public final U doTransform(Message<?> message) {
		return transformPayload((T) message.getPayload());
	}

	protected abstract U transformPayload(T payload);

}
