/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.messaging.Message;

/**
 * A base class for {@link Transformer} implementations.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public abstract class AbstractTransformer extends IntegrationObjectSupport implements Transformer {

	@Override
	public final Message<?> transform(Message<?> message) {
		try {
			Object result = this.doTransform(message);
			if (result == null) {
				return null;
			}
			return (result instanceof Message) ? (Message<?>) result
					: getMessageBuilderFactory().withPayload(result).copyHeaders(message.getHeaders()).build();
		}
		catch (MessageTransformationException e) { // NOSONAR - catch and throw
			throw e;
		}
		catch (Exception e) {
			throw new MessageTransformationException(message, "failed to transform message", e);
		}
	}

	/**
	 * Subclasses must implement this method to provide the transformation
	 * logic. If the return value is itself a Message, it will be used as the
	 * result. Otherwise, any non-null return value will be used as the payload
	 * of the result Message.
	 *
	 * @param message The message.
	 * @return The result of the transformation.
	 */
	protected abstract Object doTransform(Message<?> message);

}
