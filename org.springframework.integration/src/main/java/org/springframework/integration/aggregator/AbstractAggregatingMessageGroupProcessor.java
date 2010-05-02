/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.aggregator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Base class for MessageGroupProcessor implementations that aggregate the group
 * of Messages into a single Message.
 * 
 * @author Iwein Fuld
 * @author Alexander Peters
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class AbstractAggregatingMessageGroupProcessor implements MessageGroupProcessor {

	private final Log logger = LogFactory.getLog(this.getClass());

	@SuppressWarnings("unchecked")
	public final void processAndSend(MessageGroup group, MessageChannelTemplate channelTemplate,
			MessageChannel outputChannel) {
		Assert.notNull(group, "MessageGroup must not be null");
		Assert.notNull(outputChannel, "'outputChannel' must not be null");
		Object payload = this.aggregatePayloads(group);
		Map<String, Object> headers = this.aggregateHeaders(group);
		MessageBuilder<?> builder = (payload instanceof Message)
				? MessageBuilder.fromMessage((Message<?>) payload)
				: MessageBuilder.withPayload(payload);
		Message<?> message = builder.copyHeadersIfAbsent(headers).build();
		channelTemplate.send(message, outputChannel);
	}

	/**
	 * This default implementation simply returns all headers that have no
	 * conflicts among the group. An absent header on one or more Messages
	 * within the group is not considered a conflict. Subclasses may override
	 * this method with more advanced conflict-resolution strategies if
	 * necessary.
	 */
	protected Map<String, Object> aggregateHeaders(MessageGroup group) {
		Map<String, Object> aggregatedHeaders = new HashMap<String, Object>();
		Set<String> conflictKeys = new HashSet<String>();
		for (Message<?> message : group.getUnmarked()) {
			MessageHeaders currentHeaders = message.getHeaders();
			for (String key : currentHeaders.keySet()) {
				if (MessageHeaders.ID.equals(key) || MessageHeaders.TIMESTAMP.equals(key)
						|| MessageHeaders.SEQUENCE_SIZE.equals(key)) {
					continue;
				}
				Object value = currentHeaders.get(key);
				if (!aggregatedHeaders.containsKey(key)) {
					aggregatedHeaders.put(key, value);
				}
				else if (!value.equals(aggregatedHeaders.get(key))) {
					conflictKeys.add(key);
				}
			}
		}
		for (String keyToRemove : conflictKeys) {
			if (logger.isInfoEnabled()) {
				logger.info("Excluding header '" + keyToRemove + "' upon aggregation due to conflict(s) "
						+ "in MessageGroup with correlation key: " + group.getCorrelationKey());
			}
			aggregatedHeaders.remove(keyToRemove);
		}
		return aggregatedHeaders;
	}

	protected abstract Object aggregatePayloads(MessageGroup group);

}
