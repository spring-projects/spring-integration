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

package org.springframework.integration.debezium.support;

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
 * Specifies how to convert Debezium {@link io.debezium.engine.ChangeEvent#headers()} into a {@link MessageHeaders}.
 *
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class DefaultDebeziumHeaderMapper implements HeaderMapper<List<Header<Object>>> {

	private String[] headerNamesToMap = {"*"};

	/**
	 * Comma-separated list of names of Debezium's Change Event headers to be mapped to the outbound Message headers.
	 * The Debezium New Record State Extraction 'add.headers' property is used to configure the metadata to be set in
	 * the produced ChangeEvent headers. Note that you must prefix the 'headerNames' used the 'setHeaderNamesToMap'
	 * with the prefix configured by the 'add.headers.prefix' debezium property. Later defaults to '__'. For example for
	 * 'add.headers=op,name' and 'add.headers.prefix=__' you should use headerNames == "__op", "__name".
	 * @param headerNames The values in this list can be a simple patterns to be matched against the header names.
	 * @see <a href="https://debezium.io/documentation/reference/transformations/event-flattening.html#extract-new-record-state-add-headers">add.headers</a>
	 * @see <a href="https://debezium.io/documentation/reference/transformations/event-flattening.html#extract-new-record-state-add-headers-prefix">add.headers.prefix</a>
	 */
	public void setHeaderNamesToMap(String... headerNames) {
		Assert.notNull(headerNames, "'HeaderNames' must not be null.");
		Assert.noNullElements(headerNames, "'HeaderNames' must not contains null elements.");
		String[] copy = Arrays.copyOf(headerNames, headerNames.length);
		Arrays.sort(copy);
		this.headerNamesToMap = copy;
	}

	@Override
	public MessageHeaders toHeaders(List<Header<Object>> debeziumHeaders) {
		Map<String, Object> messageHeaders = null;
		if (!CollectionUtils.isEmpty(debeziumHeaders)) {
			messageHeaders = new HashMap<>();
			for (Header<Object> header : debeziumHeaders) {
				String headerName = header.getKey();
				if (shouldMapHeader(headerName, this.headerNamesToMap)) {
					Object headerValue = header.getValue();
					messageHeaders.put(headerName, headerValue);
				}
			}
		}
		return new MessageHeaders(messageHeaders);
	}

	@Override
	public void fromHeaders(MessageHeaders headers, List<Header<Object>> target) {
		throw new UnsupportedOperationException("The 'fromHeaders' is not supported!");
	}

	private static boolean shouldMapHeader(String headerName, String[] patterns) {
		if (patterns.length > 0) {
			for (String pattern : patterns) {
				if (PatternMatchUtils.simpleMatch(pattern, headerName)) {
					return true;
				}
			}
		}

		return false;
	}

}
