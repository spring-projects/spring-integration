/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.support.converter;

import org.springframework.messaging.Message;
import org.springframework.messaging.converter.StringMessageConverter;

/**
 * A {@link StringMessageConverter} extension to convert any object to string.
 * <p>
 * Delegates to super when payload is {@code byte[]} or {@code String}.
 * Performs {@link Object#toString()} in other cases.
 *
 * @author Marius Bogoevici
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ObjectStringMessageConverter extends StringMessageConverter {

	@Override
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
		Object payload = message.getPayload();
		if (payload instanceof String || payload instanceof byte[]) {
			return super.convertFromInternal(message, targetClass, conversionHint);
		}
		else {
			return payload.toString();
		}
	}

}
