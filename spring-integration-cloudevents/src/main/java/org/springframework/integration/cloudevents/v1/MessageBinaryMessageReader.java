/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.cloudevents.v1;

import java.util.Map;
import java.util.function.BiConsumer;

import io.cloudevents.SpecVersion;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.core.impl.StringUtils;
import io.cloudevents.core.message.impl.BaseGenericBinaryMessageReaderImpl;

/**
 * Utility for converting maps (message headers) to `CloudEvent` contexts.
 *
 * @author Dave Syer
 * @author Glenn Renfro
 *
 * @since 7.0
 *
 */
public class MessageBinaryMessageReader extends BaseGenericBinaryMessageReaderImpl<String, Object> {
	private final String cePrefix;

	private final Map<String, Object> headers;

	public MessageBinaryMessageReader(SpecVersion version, Map<String, Object> headers, byte[] payload, String cePrefix) {
		super(version, payload == null ? null : BytesCloudEventData.wrap(payload));
		this.headers = headers;
		this.cePrefix = cePrefix;
	}

	public MessageBinaryMessageReader(SpecVersion version, Map<String, Object> headers, String cePrefix) {
		this(version, headers, null, cePrefix);
	}

	@Override
	protected boolean isContentTypeHeader(String key) {
		return org.springframework.messaging.MessageHeaders.CONTENT_TYPE.equalsIgnoreCase(key);
	}

	@Override
	protected boolean isCloudEventsHeader(String key) {
		return key != null && key.length() > this.cePrefix.length() && StringUtils.startsWithIgnoreCase(key, this.cePrefix);
	}

	@Override
	protected String toCloudEventsKey(String key) {
		return key.substring(this.cePrefix.length()).toLowerCase();
	}

	@Override
	protected void forEachHeader(BiConsumer<String, Object> fn) {
		this.headers.forEach((k, v) -> fn.accept(k, v));
	}

	@Override
	protected String toCloudEventsValue(Object value) {
		return value.toString();
	}

}
