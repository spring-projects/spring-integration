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

package org.springframework.integration.amqp.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * An {@link ErrorMessageStrategy} extension that adds the raw AMQP message as
 * a header to the {@link ErrorMessage}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3.10
 *
 */
public class AmqpMessageHeaderErrorMessageStrategy implements ErrorMessageStrategy {

	/**
	 * Header name/retry context variable for the raw received message.
	 */
	public static final String AMQP_RAW_MESSAGE = AmqpHeaders.PREFIX + "raw_message";

	@Override
	public ErrorMessage buildErrorMessage(Throwable throwable, @Nullable AttributeAccessor context) {
		Object inputMessage = context == null ? null
				: context.getAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY);
		Map<String, Object> headers = new HashMap<>();
		if (context != null) {
			headers.put(AMQP_RAW_MESSAGE, context.getAttribute(AMQP_RAW_MESSAGE));
			headers.put(IntegrationMessageHeaderAccessor.SOURCE_DATA, context.getAttribute(AMQP_RAW_MESSAGE));
		}
		if (inputMessage instanceof Message) {
			return new ErrorMessage(throwable, headers, (Message<?>) inputMessage);
		}
		else {
			return new ErrorMessage(throwable, headers);
		}
	}

}
