/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
