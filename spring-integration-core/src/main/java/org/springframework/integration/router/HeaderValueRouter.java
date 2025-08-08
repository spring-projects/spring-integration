/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router;

import java.util.Collections;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A Message Router that resolves the MessageChannel from a header value.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 1.0.3
 */
public class HeaderValueRouter extends AbstractMappingMessageRouter {

	private final String headerName;

	/**
	 * Create a router that uses the provided header name to lookup a channel.
	 *
	 * @param headerName The header name.
	 */
	public HeaderValueRouter(String headerName) {
		Assert.notNull(headerName, "'headerName' must not be null");
		this.headerName = headerName;
	}

	@Override
	protected List<Object> getChannelKeys(Message<?> message) {
		Object value = message.getHeaders().get(this.headerName);
		if (value instanceof String && ((String) value).indexOf(',') != -1) {
			value = StringUtils.tokenizeToStringArray((String) value, ",");
		}
		return Collections.singletonList(value);
	}

}
