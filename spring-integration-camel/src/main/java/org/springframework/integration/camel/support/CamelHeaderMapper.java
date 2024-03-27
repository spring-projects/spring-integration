/*
 * Copyright 2022-2024 the original author or authors.
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

package org.springframework.integration.camel.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Message;

import org.springframework.core.log.LogAccessor;
import org.springframework.core.log.LogMessage;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * A {@link HeaderMapper} for mapping headers from Spring Integration message
 * to Apache Camel message and back.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class CamelHeaderMapper implements HeaderMapper<org.apache.camel.Message> {

	private static final LogAccessor LOGGER = new LogAccessor(CamelHeaderMapper.class);

	private String[] inboundHeaderNames = {"*"};

	private String[] outboundHeaderNames = {"*"};

	/**
	 * Provide a list of patterns to map Apache Camel message headers into Spring Integration message.
	 * By default, it maps all.
	 * @param inboundHeaderNames the Apache Camel message headers patterns to map.
	 */
	public void setInboundHeaderNames(String... inboundHeaderNames) {
		Assert.notNull(inboundHeaderNames, "'inboundHeaderNames' must not be null");
		String[] copy = Arrays.copyOf(inboundHeaderNames, inboundHeaderNames.length);
		Arrays.sort(copy);
		this.inboundHeaderNames = copy;
	}

	/**
	 * Provide a list of patterns to map Spring Integration message headers into an Apache Camel message.
	 * By default, it maps all.
	 * @param outboundHeaderNames the header patterns to map.
	 */
	public void setOutboundHeaderNames(String... outboundHeaderNames) {
		Assert.notNull(outboundHeaderNames, "'outboundHeaderNames' must not be null");
		String[] copy = Arrays.copyOf(outboundHeaderNames, outboundHeaderNames.length);
		Arrays.sort(copy);
		this.outboundHeaderNames = copy;
	}

	@Override
	public void fromHeaders(MessageHeaders headers, Message target) {
		for (Map.Entry<String, Object> entry : headers.entrySet()) {
			String name = entry.getKey();
			if (shouldMapHeader(name, this.outboundHeaderNames)) {
				Object value = entry.getValue();
				if (value != null) {
					target.setHeader(name, value);
				}
			}
		}
	}

	@Override
	public Map<String, Object> toHeaders(Message source) {
		Map<String, Object> headers = new HashMap<>();
		for (Map.Entry<String, Object> entry : source.getHeaders().entrySet()) {
			String name = entry.getKey();
			if (shouldMapHeader(name, this.inboundHeaderNames)) {
				Object value = entry.getValue();
				if (value != null) {
					headers.put(name, value);
				}
			}
		}
		return headers;
	}

	private static boolean shouldMapHeader(String headerName, String[] patterns) {
		if (patterns.length > 0) {
			for (String pattern : patterns) {
				if (PatternMatchUtils.simpleMatch(pattern, headerName)) {
					LOGGER.debug(LogMessage.format("headerName=[{0}] WILL be mapped, matched pattern={1}",
							headerName, pattern));
					return true;
				}
			}
		}
		LOGGER.debug(LogMessage.format("headerName=[{0}] WILL NOT be mapped", headerName));
		return false;
	}

}
