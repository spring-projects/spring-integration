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

package org.springframework.integration.transformer;

import java.util.UUID;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageStore;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class ClaimCheckTransformer extends AbstractTransformer {

	private final MessageStore messageStore;


	public ClaimCheckTransformer(MessageStore messageStore) {
		Assert.notNull(messageStore, "MessageStore must not be null");
		this.messageStore = messageStore;
	}


	@Override
	protected Object doTransform(Message<?> message) throws Exception {
		Assert.notNull(message, "message must not be null");
		Object payload = message.getPayload();
		Assert.notNull(payload, "payload must not be null");
		MessageBuilder<?> responseBuilder = null;
		if (payload instanceof UUID) {
			Message<?> original = this.retrieveMessage((UUID) payload);
			responseBuilder = MessageBuilder.fromMessage(original);
		}
		else {
			this.storeMessage(message);
			responseBuilder = MessageBuilder.withPayload(message.getHeaders().getId());
		}
		// headers on the 'current' message take precedence
		responseBuilder.copyHeaders(message.getHeaders());
		return responseBuilder.build();
	}


	private Message<?> retrieveMessage(UUID id) {
		Message<?> result = this.messageStore.getMessage(id);
		Assert.notNull(result, "unable to locate Message for claim check ID: " + id);
		return result;
	}

	private void storeMessage(Message<?> message) {
		this.messageStore.addMessage(message);
	}

}
