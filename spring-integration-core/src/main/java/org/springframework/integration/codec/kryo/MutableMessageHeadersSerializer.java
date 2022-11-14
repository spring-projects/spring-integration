/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.integration.codec.kryo;

import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.MessageHeaders;

/**
 * Kryo Serializer for {@link MutableMessageHeaders}.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 4.2
 */
class MutableMessageHeadersSerializer extends MessageHeadersSerializer {

	@Override
	public MessageHeaders read(Kryo kryo, Input input, Class<? extends MessageHeaders> type) {
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = kryo.readObject(input, HashMap.class);
		return new MutableMessageHeaders(headers);
	}

}
