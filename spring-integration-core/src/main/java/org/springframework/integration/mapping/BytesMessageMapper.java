/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.integration.mapping;

import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * An {@link OutboundMessageMapper} and {@link InboundMessageMapper} that
 * maps to/from {@code byte[]}.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public interface BytesMessageMapper extends InboundMessageMapper<byte[]>, OutboundMessageMapper<byte[]> {

	@Override
	@NonNull // override
	default Message<?> toMessage(byte[] object) {
		return toMessage(object, null);
	}

	@Override
	@NonNull
		// override
	Message<?> toMessage(byte[] bytes, @Nullable Map<String, Object> headers);

}
