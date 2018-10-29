/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.integration.kafka.support;

import java.util.Collections;
import java.util.Map;

import org.springframework.core.AttributeAccessor;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * {@link ErrorMessageStrategy} extension that adds the raw record as
 * a header to the {@link ErrorMessage}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1.1
 *
 */
public class RawRecordHeaderErrorMessageStrategy implements ErrorMessageStrategy {

	@Override
	public ErrorMessage buildErrorMessage(Throwable throwable, @Nullable AttributeAccessor context) {
		Object inputMessage = context.getAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY);
		Map<String, Object> headers =
				Collections.singletonMap(KafkaHeaders.RAW_DATA, context.getAttribute(KafkaHeaders.RAW_DATA));
		if (inputMessage instanceof Message) {
			return new ErrorMessage(throwable, headers, (Message<?>) inputMessage);
		}
		else {
			return new ErrorMessage(throwable, headers);
		}
	}

}
