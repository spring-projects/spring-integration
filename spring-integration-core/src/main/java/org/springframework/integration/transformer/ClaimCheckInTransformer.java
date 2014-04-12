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

package org.springframework.integration.transformer;

import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Transformer that stores a Message and returns a new Message whose payload
 * is the id of the stored Message.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class ClaimCheckInTransformer extends AbstractTransformer {

	private final MessageStore messageStore;


	/**
	 * Create a claim check-in transformer that will delegate to the provided MessageStore.
	 *
	 * @param messageStore The message store.
	 */
	public ClaimCheckInTransformer(MessageStore messageStore) {
		Assert.notNull(messageStore, "MessageStore must not be null");
		this.messageStore = messageStore;
	}

	@Override
	public String getComponentType() {
		return "claim-check-in";
	}

	@Override
	protected Object doTransform(Message<?> message) throws Exception {
		Assert.notNull(message, "message must not be null");
		Object payload = message.getPayload();
		Assert.notNull(payload, "payload must not be null");
		Message<?> storedMessage = this.messageStore.addMessage(message);
		AbstractIntegrationMessageBuilder<?> responseBuilder = this.getMessageBuilderFactory().withPayload(storedMessage.getHeaders().getId());
		// headers on the 'current' message take precedence
		responseBuilder.copyHeaders(message.getHeaders());
		return responseBuilder.build();
	}

}
