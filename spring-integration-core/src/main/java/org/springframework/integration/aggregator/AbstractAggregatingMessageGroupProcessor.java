/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.core.MessagingOperations;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Base class for MessageGroupProcessor implementations that aggregate the group of Messages into a single Message.
 * 
 * @author Iwein Fuld
 * @author Alexander Peters
 * @author Mark Fisher
 * @author Dave Syer
 * 
 * @since 2.0
 */
public abstract class AbstractAggregatingMessageGroupProcessor implements MessageGroupProcessor {

	private final Log logger = LogFactory.getLog(this.getClass());

	public final void processAndSend(MessageGroup group, MessagingOperations messagingTemplate, MessageChannel outputChannel) {
		Assert.notNull(group, "MessageGroup must not be null");
		Assert.notNull(outputChannel, "'outputChannel' must not be null");
		Map<String, Object> headers = this.aggregateHeaders(group);
		Object payload = this.aggregatePayloads(group, headers);
		MessageBuilder<?> builder;
		if (payload instanceof Message<?>) {
			builder = MessageBuilder.fromMessage((Message<?>) payload);
		}
		else {
			builder = MessageBuilder.withPayload(payload).copyHeadersIfAbsent(headers);
		}
		Message<?> message = builder.build();
		messagingTemplate.send(outputChannel, message);
	}

	/**
	 * This default implementation simply returns all headers that have no conflicts among the group. An absent header
	 * on one or more Messages within the group is not considered a conflict. Subclasses may override this method with
	 * more advanced conflict-resolution strategies if necessary.
	 */
	protected Map<String, Object> aggregateHeaders(MessageGroup group) {
		Map<String, Object> aggregatedHeaders = new HashMap<String, Object>();
		Set<String> conflictKeys = new HashSet<String>();
		for (Message<?> message : group.getUnmarked()) {
			MessageHeaders currentHeaders = message.getHeaders();
			for (String key : currentHeaders.keySet()) {
				if (MessageHeaders.ID.equals(key) || MessageHeaders.TIMESTAMP.equals(key)
						|| MessageHeaders.SEQUENCE_SIZE.equals(key) || MessageHeaders.SEQUENCE_NUMBER.equals(key)
						|| MessageHeaders.CORRELATION_ID.equals(key)) {
					continue;
				}
				if (AbstractMessageSplitter.SEQUENCE_DETAILS.equals(key)
						&& !aggregatedHeaders.containsKey(MessageHeaders.CORRELATION_ID)) {
					@SuppressWarnings("unchecked")
					List<Object[]> incomingSequenceDetails = new ArrayList<Object[]>(currentHeaders
							.get(key, List.class));
					Object[] sequenceDetails = incomingSequenceDetails.remove(incomingSequenceDetails.size() - 1);
					Assert.state(sequenceDetails.length == 3, "Wrong sequence details (not created by splitter?): "
							+ Arrays.asList(sequenceDetails));
					aggregatedHeaders.put(MessageHeaders.CORRELATION_ID, sequenceDetails[0]);
					Integer sequenceNumber = (Integer) sequenceDetails[1];
					Integer sequenceSize = (Integer) sequenceDetails[2];
					if (sequenceSize > 0) {
						aggregatedHeaders.put(MessageHeaders.SEQUENCE_NUMBER, sequenceNumber);
						aggregatedHeaders.put(MessageHeaders.SEQUENCE_SIZE, sequenceSize);
					}
					if (!incomingSequenceDetails.isEmpty()) {
						aggregatedHeaders.put(AbstractMessageSplitter.SEQUENCE_DETAILS, incomingSequenceDetails);
					}
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
			if (logger.isDebugEnabled()) {
				logger.debug("Excluding header '" + keyToRemove + "' upon aggregation due to conflict(s) "
						+ "in MessageGroup with correlation key: " + group.getGroupId());
			}
			aggregatedHeaders.remove(keyToRemove);
		}
		return aggregatedHeaders;
	}

	protected abstract Object aggregatePayloads(MessageGroup group, Map<String, Object> defaultHeaders);

}
