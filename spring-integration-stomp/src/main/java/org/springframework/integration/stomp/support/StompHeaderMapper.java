/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.integration.stomp.support;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.log.LogAccessor;
import org.springframework.http.MediaType;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * The STOMP {@link HeaderMapper} implementation.
 *
 * @author Artem Bilan
 *
 * @since 4.2
 *
 * @see StompHeaders
 */
public class StompHeaderMapper implements HeaderMapper<StompHeaders> {

	private static final LogAccessor LOGGER = new LogAccessor(StompHeaderMapper.class);

	public static final String STOMP_INBOUND_HEADER_NAME_PATTERN = "STOMP_INBOUND_HEADERS";

	public static final String STOMP_OUTBOUND_HEADER_NAME_PATTERN = "STOMP_OUTBOUND_HEADERS";

	private static final String[] STOMP_INBOUND_HEADER_NAMES =
			{
					StompHeaders.CONTENT_LENGTH,
					StompHeaders.CONTENT_TYPE,
					StompHeaders.MESSAGE_ID,
					StompHeaders.RECEIPT_ID,
					StompHeaders.SUBSCRIPTION,
			};

	private static final List<String> STOMP_INBOUND_HEADER_NAMES_LIST = Arrays.asList(STOMP_INBOUND_HEADER_NAMES);

	private static final String[] STOMP_OUTBOUND_HEADER_NAMES =
			{
					StompHeaders.CONTENT_LENGTH,
					StompHeaders.CONTENT_TYPE,
					StompHeaders.DESTINATION,
					StompHeaders.RECEIPT,
					IntegrationStompHeaders.DESTINATION,
					IntegrationStompHeaders.RECEIPT
			};

	private static final List<String> STOMP_OUTBOUND_HEADER_NAMES_LIST = Arrays.asList(STOMP_OUTBOUND_HEADER_NAMES);

	private String[] inboundHeaderNames = STOMP_INBOUND_HEADER_NAMES;

	private String[] outboundHeaderNames = STOMP_OUTBOUND_HEADER_NAMES;

	public void setInboundHeaderNames(String[] inboundHeaderNames) {
		Assert.notNull(inboundHeaderNames, "'inboundHeaderNames' must not be null.");
		Assert.noNullElements(inboundHeaderNames, "'inboundHeaderNames' must not contains null elements.");
		String[] copy = Arrays.copyOf(inboundHeaderNames, inboundHeaderNames.length);
		Arrays.sort(copy);
		if (!Arrays.equals(STOMP_INBOUND_HEADER_NAMES, inboundHeaderNames)) {
			this.inboundHeaderNames = copy;
		}
	}

	public void setOutboundHeaderNames(String[] outboundHeaderNames) {
		Assert.notNull(outboundHeaderNames, "'outboundHeaderNames' must not be null.");
		Assert.noNullElements(outboundHeaderNames, "'outboundHeaderNames' must not contains null elements.");
		String[] copy = Arrays.copyOf(outboundHeaderNames, outboundHeaderNames.length);
		Arrays.sort(copy);
		if (!Arrays.equals(STOMP_OUTBOUND_HEADER_NAMES, outboundHeaderNames)) {
			this.outboundHeaderNames = copy;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void fromHeaders(MessageHeaders headers, StompHeaders target) {
		for (Map.Entry<String, Object> entry : headers.entrySet()) {
			String name = entry.getKey();
			if (shouldMapHeader(name, this.outboundHeaderNames)) {
				Object value = entry.getValue();
				if (value != null) {
					setStompHeader(target, name, value);
				}
			}
			else if (StompHeaderAccessor.NATIVE_HEADERS.equals(name)) {
				MultiValueMap<String, String> multiValueMap =
						headers.get(StompHeaderAccessor.NATIVE_HEADERS, MultiValueMap.class);
				if (multiValueMap != null) {
					for (Map.Entry<String, List<String>> entry1 : multiValueMap.entrySet()) {
						name = entry1.getKey();
						if (shouldMapHeader(name, this.outboundHeaderNames)) {
							String value = entry1.getValue().get(0);
							if (StringUtils.hasText(value)) {
								setStompHeader(target, name, value);
							}
						}
					}
				}
			}
		}
	}

	private void setStompHeader(StompHeaders target, String name, Object value) {
		switch (name) {
			case StompHeaders.CONTENT_LENGTH:
				setContentLength(target, value);
				break;

			case StompHeaders.CONTENT_TYPE:
			case MessageHeaders.CONTENT_TYPE:
				setContentType(target, name, value);
				break;

			case StompHeaders.DESTINATION:
			case IntegrationStompHeaders.DESTINATION:
				setDestination(target, value);
				break;

			case StompHeaders.RECEIPT:
			case IntegrationStompHeaders.RECEIPT:
				setReceipt(target, value);
				break;

			default:
				if (value instanceof String) {
					target.set(name, (String) value);
				}
				else {
					Class<?> clazz = (value != null) ? value.getClass() : null;
					throw new IllegalArgumentException(
							"Expected String value for any generic STOMP header value, but received: " + clazz);
				}
		}
	}

	private static void setContentLength(StompHeaders target, Object value) {
		if (value instanceof Number) {
			target.setContentLength(((Number) value).longValue());
		}
		else if (value instanceof String) {
			target.setContentLength(Long.parseLong((String) value));
		}
		else {
			Class<?> clazz = (value != null) ? value.getClass() : null;
			throw new IllegalArgumentException(
					"Expected Number or String value for 'content-length' header value, but received: " + clazz);
		}
	}

	private static void setContentType(StompHeaders target, String name, Object value) {
		MimeType contentType = target.getContentType();
		if (contentType == null || StompHeaders.CONTENT_TYPE.equals(name)) {
			if (value instanceof MediaType) {
				target.setContentType((MediaType) value);
			}
			else if (value instanceof String) {
				target.setContentType(MediaType.parseMediaType((String) value));
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected MediaType or String value for 'content-type' header value, but received: "
								+ clazz);
			}
		}
	}

	private static void setReceipt(StompHeaders target, Object value) {
		if (value instanceof String) {
			target.setReceipt((String) value);
		}
		else {
			Class<?> clazz = (value != null) ? value.getClass() : null;
			throw new IllegalArgumentException(
					"Expected String value for 'receipt' header value, but received: " + clazz);
		}
	}

	private static void setDestination(StompHeaders target, Object value) {
		if (value instanceof String) {
			target.setDestination((String) value);
		}
		else {
			Class<?> clazz = (value != null) ? value.getClass() : null;
			throw new IllegalArgumentException(
					"Expected String value for 'destination' header value, but received: " + clazz);
		}
	}

	@Override
	public Map<String, Object> toHeaders(StompHeaders source) {
		Map<String, Object> target = new HashMap<>();
		for (String name : source.keySet()) {
			if (shouldMapHeader(name, this.inboundHeaderNames)) {
				if (StompHeaders.CONTENT_TYPE.equals(name)) {
					target.put(MessageHeaders.CONTENT_TYPE, source.getContentType());
				}
				else {
					String key = name;
					if (IntegrationStompHeaders.HEADERS.contains(name)) {
						key = IntegrationStompHeaders.PREFIX + name;
					}
					target.put(key, source.getFirst(name));
				}
			}
		}
		return target;
	}


	private static boolean shouldMapHeader(String headerName, String[] patterns) {
		if (patterns != null && patterns.length > 0) {
			for (String pattern : patterns) {
				if (PatternMatchUtils.simpleMatch(pattern, headerName)) {
					LOGGER.debug(() -> MessageFormat.format("headerName=[{0}] WILL be mapped, matched pattern={1}",
							headerName, pattern));
					return true;
				}
				else if (STOMP_INBOUND_HEADER_NAME_PATTERN.equals(pattern)
						&& STOMP_INBOUND_HEADER_NAMES_LIST.contains(headerName)) {

					LOGGER.debug(() -> MessageFormat.format("headerName=[{0}] WILL be mapped, matched pattern={1}",
							headerName, pattern));
					return true;
				}
				else if (STOMP_OUTBOUND_HEADER_NAME_PATTERN.equals(pattern)
						&& (STOMP_OUTBOUND_HEADER_NAMES_LIST.contains(headerName)
						|| MessageHeaders.CONTENT_TYPE.equals(headerName))) {

					LOGGER.debug(() -> MessageFormat.format("headerName=[{0}] WILL be mapped, matched pattern={1}",
							headerName, pattern));
					return true;
				}
			}
		}
		LOGGER.debug(() -> MessageFormat.format("headerName=[{0}] WILL NOT be mapped", headerName));
		return false;
	}

}
