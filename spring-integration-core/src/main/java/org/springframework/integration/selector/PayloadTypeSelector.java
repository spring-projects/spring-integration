/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.selector;

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link MessageSelector} implementation that checks the type of the
 * {@link Message} payload. The payload type must be assignable to at least one
 * of the selector's accepted types.
 *
 * @author Mark Fisher
 */
public class PayloadTypeSelector implements MessageSelector {

	private final List<Class<?>> acceptedTypes = new ArrayList<Class<?>>();

	/**
	 * Create a selector for the provided types. At least one is required.
	 *
	 * @param types The types.
	 */
	public PayloadTypeSelector(Class<?>... types) {
		Assert.notEmpty(types, "at least one type is required");
		for (Class<?> type : types) {
			this.acceptedTypes.add(type);
		}
	}

	@Override
	public boolean accept(Message<?> message) {
		Assert.notNull(message, "'message' must not be null");
		Object payload = message.getPayload();
		Assert.notNull(payload, "'payload' must not be null");
		for (Class<?> type : this.acceptedTypes) {
			if (type.isAssignableFrom(payload.getClass())) {
				return true;
			}
		}
		return false;
	}

}
