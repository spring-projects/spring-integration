/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
