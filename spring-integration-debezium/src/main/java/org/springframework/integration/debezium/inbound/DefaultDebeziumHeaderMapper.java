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

package org.springframework.integration.debezium.inbound;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.debezium.engine.Header;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.HeaderMapper;
import org.springframework.util.CollectionUtils;

/**
 * Specifies how to convert Debezium {@link ChangeEvent} {@link Header}s into {@link Message} headers.
 * @param <T> encoding type.
 *
 * @author Christian Tzolov
 */
public class DefaultDebeziumHeaderMapper<T> implements HeaderMapper<List<Header<T>>> {

	@Override
	public void fromHeaders(MessageHeaders headers, List<Header<T>> target) {
		throw new UnsupportedOperationException("The 'fromHeaders' is not supported!");
	}

	@Override
	public MessageHeaders toHeaders(List<Header<T>> debeziumHeaders) {
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		if (!CollectionUtils.isEmpty(debeziumHeaders)) {
			Iterator<Header<T>> itr = debeziumHeaders.iterator();
			while (itr.hasNext()) {
				Header<T> header = itr.next();
				String key = header.getKey();
				T value = header.getValue();
				messageHeaders.put(key, value);
			}
		}
		return new MessageHeaders(messageHeaders);
	}

}
