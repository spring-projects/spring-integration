/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.integration.store.MessageGroup;
import org.springframework.util.Assert;

/**
 * This implementation of MessageGroupProcessor will take the messages from the
 * MessageGroup and pass them on in a single message with a Collection as a payload.
 * 
 * @author Iwein Fuld
 * @author Alexander Peters
 * @author Mark Fisher
 * @since 2.0
 */
public class DefaultAggregatingMessageGroupProcessor extends AbstractAggregatingMessageGroupProcessor {

	@Override
	protected final Object aggregatePayloads(MessageGroup group, Map<String, Object> headers) {
		Collection<Message<?>> messages = group.getMessages();
		Assert.notEmpty(messages, this.getClass().getSimpleName() + " cannot process empty message groups");
		List<Object> payloads = new ArrayList<Object>(messages.size());
		for (Message<?> message : messages) {
			payloads.add(message.getPayload());
		}
		return payloads;
	}

}
