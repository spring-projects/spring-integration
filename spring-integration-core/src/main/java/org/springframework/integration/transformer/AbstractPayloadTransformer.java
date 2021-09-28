/*
 * Copyright 2002-2021 the original author or authors.
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
