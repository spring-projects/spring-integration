/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
		return this.transformPayload((T) message.getPayload());
	}

	protected abstract U transformPayload(T payload);

}
