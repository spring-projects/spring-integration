/*
 * Copyright 2002-2014 the original author or authors.
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
