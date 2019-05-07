/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.integration.support;

import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.validation.Validator;

/**
 * A {@link PayloadMethodArgumentResolver} that treats KafkaNull payloads as null.
 * {@link org.springframework.messaging.handler.annotation.Payload @Paylaod}
 * annotation must have required = false.
 *
 * @author Gary Russell
 * @since 5.1
 *
 */
public class NullAwarePayloadArgumentResolver extends PayloadMethodArgumentResolver {

	public NullAwarePayloadArgumentResolver(MessageConverter messageConverter) {
		super(messageConverter, null, false);
	}

	public NullAwarePayloadArgumentResolver(MessageConverter messageConverter, Validator validator) {
		super(messageConverter, validator, false);
	}

	@Override
	protected boolean isEmptyPayload(Object payload) {
		return super.isEmptyPayload(payload) || "KafkaNull".equals(payload.getClass().getSimpleName());
	}

}
