/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.transformer;

import java.util.UUID;

import org.springframework.core.log.LogMessage;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Transformer that accepts a Message whose payload is a UUID and retrieves the Message associated
 * with that id from a MessageStore if available. An Exception will be thrown if no Message with
 * that ID can be retrieved from the given MessageStore.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Nick Spacek
 * @author Artem Bilan
 * @author Trung Pham
 *
 * @since 2.0
 */
public class ClaimCheckOutTransformer extends AbstractTransformer implements IntegrationPattern {

	private final MessageStore messageStore;

	private volatile boolean removeMessage = false;

	/**
	 * Create a claim check-out transformer that will delegate to the provided MessageStore.
	 *
	 * @param messageStore The message store.
	 */
	public ClaimCheckOutTransformer(MessageStore messageStore) {
		Assert.notNull(messageStore, "MessageStore must not be null");
		this.messageStore = messageStore;
	}

	public void setRemoveMessage(boolean removeMessage) {
		this.removeMessage = removeMessage;
	}

	@Override
	public String getComponentType() {
		return "claim-check-out";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.claim_check_out;
	}

	@Override
	protected Object doTransform(Message<?> message) {
		Assert.notNull(message, "message must not be null");
		Assert.isTrue(message.getPayload() instanceof UUID, "payload must be a UUID");
		UUID id = (UUID) message.getPayload();
		Message<?> retrievedMessage;
		if (this.removeMessage) {
			retrievedMessage = this.messageStore.removeMessage(id);
			logger.debug(LogMessage.format("Removed Message with claim-check '%s' from the MessageStore.", id));
		}
		else {
			retrievedMessage = this.messageStore.getMessage(id);
		}
		Assert.notNull(retrievedMessage,
				() -> "unable to locate Message for ID: " + id + " within MessageStore [" + this.messageStore + "]");
		AbstractIntegrationMessageBuilder<?> responseBuilder = getMessageBuilderFactory().fromMessage(retrievedMessage);
		// headers on the 'current' message take precedence
		responseBuilder.copyHeaders(message.getHeaders());
		return responseBuilder.build();
	}

}
