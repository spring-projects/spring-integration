/*
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
 * @author Artem Bilan
 */
public abstract class AbstractTransformer extends IntegrationObjectSupport implements Transformer {

	@Override
	public final Message<?> transform(Message<?> message) {
		try {
			Object result = doTransform(message);
			return result instanceof Message<?> resultMessage
					? resultMessage
					: getMessageBuilderFactory().withPayload(result).copyHeaders(message.getHeaders()).build();
		}
		catch (Exception ex) {
			if (ex instanceof MessageTransformationException messageTransformationException) {
				throw messageTransformationException;
			}
			throw new MessageTransformationException(message, "failed to transform message", ex);
		}
	}

	/**
	 * Subclasses must implement this method to provide the transformation
	 * logic. If the return value is itself a Message, it will be used as the
	 * result. Otherwise, any non-null return value will be used as the payload
	 * of the result Message.
	 * @param message The message.
	 * @return The result of the transformation.
	 */
	protected abstract Object doTransform(Message<?> message);

}
