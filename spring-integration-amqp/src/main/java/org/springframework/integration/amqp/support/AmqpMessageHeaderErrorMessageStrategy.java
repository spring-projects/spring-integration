/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.amqp.support;

import java.util.Collections;
import java.util.Map;

import org.springframework.core.AttributeAccessor;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * An {@link ErrorMessageStrategy} extension that adds the raw AMQP message as
 * a header to the {@code org.springframework.integration.message.EnhancedErrorMessage}.
 *
 * @author Gary Russell
 * @since 4.3.10
 *
 */
public class AmqpMessageHeaderErrorMessageStrategy implements ErrorMessageStrategy {

	/**
	 * Header name/retry context variable for the raw received message.
	 */
	public static final String AMQP_MESSAGE = "amqp_message"; // move to AmqpHeaders.MESSAGE in 2.0

	@SuppressWarnings("deprecation")
	@Override
	public ErrorMessage buildErrorMessage(Throwable throwable, AttributeAccessor context) {
		Object inputMessage = context.getAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY);
		Map<String, Object> headers = Collections.singletonMap(
				AmqpMessageHeaderErrorMessageStrategy.AMQP_MESSAGE,
				context.getAttribute(AmqpMessageHeaderErrorMessageStrategy.AMQP_MESSAGE));
		return inputMessage instanceof Message
				? new org.springframework.integration.message.EnhancedErrorMessage(throwable, headers,
						(Message<?>) inputMessage)
				: new ErrorMessage(throwable, headers);
	}

}
