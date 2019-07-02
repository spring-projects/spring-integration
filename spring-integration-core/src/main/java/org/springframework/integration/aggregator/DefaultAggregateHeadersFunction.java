/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * The {@link Function} implementation for a default headers merging in the aggregator
 * component. It takes all the unique headers from all the messages in group and removes
 * those which are conflicted: have different values from different messages.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see AbstractAggregatingMessageGroupProcessor
 */
public class DefaultAggregateHeadersFunction implements Function<MessageGroup, Map<String, Object>> {

	private static final Log LOGGER = LogFactory.getLog(DefaultAggregateHeadersFunction.class);

	@Override
	public Map<String, Object> apply(MessageGroup messageGroup) {
		Map<String, Object> aggregatedHeaders = new HashMap<>();
		Set<String> conflictKeys = doAggregateHeaders(messageGroup, aggregatedHeaders);
		for (String keyToRemove : conflictKeys) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Excluding header '" + keyToRemove + "' upon aggregation due to conflict(s) "
						+ "in MessageGroup with correlation key: " + messageGroup.getGroupId());
			}
			aggregatedHeaders.remove(keyToRemove);
		}
		return aggregatedHeaders;
	}

	private Set<String> doAggregateHeaders(MessageGroup group, Map<String, Object> aggregatedHeaders) {
		Set<String> conflictKeys = new HashSet<>();
		for (Message<?> message : group.getMessages()) {
			for (Map.Entry<String, Object> entry : message.getHeaders().entrySet()) {
				String key = entry.getKey();
				if (MessageHeaders.ID.equals(key)
						|| MessageHeaders.TIMESTAMP.equals(key)
						|| IntegrationMessageHeaderAccessor.SEQUENCE_SIZE.equals(key)
						|| IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER.equals(key)) {
					continue;
				}
				Object value = entry.getValue();
				if (!aggregatedHeaders.containsKey(key)) {
					aggregatedHeaders.put(key, value);
				}
				else {
					if (!Objects.equals(value, aggregatedHeaders.get(key))) {
						conflictKeys.add(key);
					}
				}
			}
		}
		return conflictKeys;
	}

}
