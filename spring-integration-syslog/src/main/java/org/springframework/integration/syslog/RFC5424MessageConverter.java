/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.integration.syslog;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link MessageConverter}; delegates to a {@link RFC5424SyslogParser} if
 * necessary (TCP will have already done the syslog conversion because it needs
 * to handle different message framing). Copies the resulting {@link Map} to
 * the message headers if {@link #asMap()} is false.
 *
 * @author Gary Russell
 * @since 4.1.1
 */
public class RFC5424MessageConverter extends DefaultMessageConverter {

	private final RFC5424SyslogParser parser;

	private String charset = "UTF-8";

	/**
	 * Construct an instance with a default {@link RFC5424SyslogParser}.
	 */
	public RFC5424MessageConverter() {
		this(new RFC5424SyslogParser());
	}

	/**
	 * Construct an instance with a non-standard parser.
	 * @param parser the parser.
	 */
	public RFC5424MessageConverter(RFC5424SyslogParser parser) {
		this.parser = parser;
	}

	/**
	 * @param charset the charset to set
	 */
	protected void setCharset(String charset) {
		this.charset = charset;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Message<?> fromSyslog(Message<?> message) {
		boolean isMap = message.getPayload() instanceof Map;
		Map<String, ?> map;
		Object originalContent;
		if (!isMap) {
			Assert.isInstanceOf(byte[].class, message.getPayload(), "Only byte[] and Map payloads are supported");
			try {
				map = this.parser.parse(new String(((byte[]) message.getPayload()), this.charset), 0, false);
			}
			catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
			originalContent = message.getPayload();
		}
		else {
			map = (Map<String, ?>) message.getPayload();
			originalContent = map.get(SyslogHeaders.UNDECODED);
			if (originalContent == null) {
				originalContent = map;
			}
		}

		AbstractIntegrationMessageBuilder<Object> builder = getMessageBuilderFactory().withPayload(
						asMap() ? map : originalContent)
				.copyHeaders(message.getHeaders());
		if (!asMap() && isMap) {
			builder.copyHeaders(map);
		}
		return builder.build();
	}

}
