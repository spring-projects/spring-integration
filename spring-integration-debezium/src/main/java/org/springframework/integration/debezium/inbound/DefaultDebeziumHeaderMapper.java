/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.integration.debezium.inbound;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.debezium.engine.Header;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.HeaderMapper;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * Specifies how to convert Debezium {@link ChangeEvent} {@link Header}s into {@link Message} headers.
 *
 * @author Christian Tzolov
 * @since 6.2
 */
public class DefaultDebeziumHeaderMapper implements HeaderMapper<List<Header<Object>>> {

	public static final String DEBEZIUM_INBOUND_HEADER_NAME_PATTERN = "DEBEZIUM_INBOUND_HEADERS";

	private static final String[] DEBEZIUM_HEADER_NAMES = {
			DebeziumHeaders.DESTINATION,
			DebeziumHeaders.KEY
	};

	private static final List<String> DEBEZIUM_HEADER_NAMES_LIST = Arrays.asList(DEBEZIUM_HEADER_NAMES);

	private String[] allowedHeaderNames = DEBEZIUM_HEADER_NAMES;

	@Override
	public void fromHeaders(MessageHeaders headers, List<Header<Object>> target) {
		throw new UnsupportedOperationException("The 'fromHeaders' is not supported!");
	}

	@Override
	public MessageHeaders toHeaders(List<Header<Object>> debeziumHeaders) {
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		if (!CollectionUtils.isEmpty(debeziumHeaders)) {
			for (Header<Object> header : debeziumHeaders) {
				String headerName = header.getKey();
				if (this.shouldMapHeader(headerName, this.allowedHeaderNames)) {
					Object headerValue = header.getValue();
					messageHeaders.put(headerName, headerValue);
				}
			}
		}
		return new MessageHeaders(messageHeaders);
	}

	/**
	 * Comma-separated list of names of Debezium's Change Event headers to be mapped to the outbound Message headers.
	 *
	 * @param headerNames The values in this list can be a simple patterns to be matched against the header names (e.g.
	 * "foo*" or "*foo"). Special token 'DEBEZIUM_INBOUND_HEADERS' represent all the standard Debezium headers for the
	 * inbound channel adapter; they are included by default. If you wish to add your own headers, you must also include
	 * this token if you wish the standard headers to also be mapped or provide your own 'HeaderMapper' implementation
	 * using 'header-mapper'.
	 */
	public void setAllowedHeaderNames(String... headerNames) {
		Assert.notNull(headerNames, "'HeaderNames' must not be null.");
		Assert.noNullElements(headerNames, "'HeaderNames' must not contains null elements.");
		String[] copy = Arrays.copyOf(headerNames, headerNames.length);
		Arrays.sort(copy);
		if (!Arrays.equals(DEBEZIUM_HEADER_NAMES, headerNames)) {
			this.allowedHeaderNames = copy;
		}
	}

	private boolean shouldMapHeader(String headerName, String[] patterns) {
		if (patterns != null && patterns.length > 0) {
			for (String pattern : patterns) {
				if (PatternMatchUtils.simpleMatch(pattern, headerName)) {
					return true;
				}
				else if (DEBEZIUM_INBOUND_HEADER_NAME_PATTERN.equals(pattern)
						&& DEBEZIUM_HEADER_NAMES_LIST.contains(headerName)) {
					return true;
				}
			}
		}

		return false;
	}

}
