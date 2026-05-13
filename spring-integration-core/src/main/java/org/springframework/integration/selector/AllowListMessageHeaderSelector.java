/*
 * Copyright 2026-present the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.messaging.Message;
import org.springframework.util.ObjectUtils;

/**
 * The {@link MessageSelector} implementation to apply patterns on the value
 * of the specified header in the message.
 * If no header value, the message is accepted by default.
 * Set {@link #setAcceptNulls(boolean)} to {@code false} to reject messages with null header values.
 * Otherwise, it is accepted only if the header value matches one of the patterns positively.
 * <p>
 * This selector can be used in any scenario where the message's metadata should be trusted
 * before it is used in the downstream flow.
 *
 * @author Artem Bilan
 *
 * @since 6.5.9
 *
 * @see PatternMatchUtils
 * @see MessageSelectorChain
 * @see org.springframework.integration.channel.interceptor.MessageSelectingInterceptor
 * @see org.springframework.integration.filter.MessageFilter
 */
public class AllowListMessageHeaderSelector implements MessageSelector {

	private final String header;

	private final String[] patterns;

	private boolean acceptNulls = true;

	/**
	 * Create an instance with the specified header and patterns.
	 * @param header the message header to validate.
	 * @param patterns the simple patterns to match ignoring a case;
	 * also supports negated ('!') patterns.
	 * First match wins (positive or negative).
	 */
	public AllowListMessageHeaderSelector(String header, String... patterns) {
		this.header = header;
		this.patterns = Arrays.copyOf(patterns, patterns.length);
	}

	/**
	 * Whether null values from the header are considered as valid.
	 * @param acceptNulls false to not accept null values.
	 */
	public void setAcceptNulls(boolean acceptNulls) {
		this.acceptNulls = acceptNulls;
	}

	@Override
	public boolean accept(Message<?> message) {
		Object headerValue = message.getHeaders().get(this.header);
		if (headerValue == null) {
			return this.acceptNulls;
		}

		String headerValueAsString = headerValueAsString(headerValue);

		Boolean matchResult = PatternMatchUtils.smartMatchIgnoreCase(headerValueAsString, this.patterns);
		return Boolean.TRUE.equals(matchResult);
	}

	private static String headerValueAsString(Object headerValue) {
		if (headerValue instanceof Class<?> aClass) {
			return aClass.getName();
		}
		else if (headerValue instanceof byte[] bytes) {
			return new String(bytes, StandardCharsets.UTF_8);
		}
		else if (headerValue instanceof char[] chars) {
			return new String(chars);
		}

		return ObjectUtils.nullSafeToString(headerValue);
	}

}
