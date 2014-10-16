/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.filter;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The {@link MessageFilter} implementation for the
 * <a href="http://www.eaipatterns.com/IdempotentReceiver.html">Idempotent Receiver</a>
 * EIP functional pattern.
 * <p>
 * By default it works like a generic {@link MessageFilter}: if message isn't accepted
 * it discarded to the {@code discardChannel} or
 * a {@link org.springframework.integration.MessageRejectedException} is thrown.
 * However with {@code filter = false} the message is sent as normal, but with
 * {@link IntegrationMessageHeaderAccessor#DUPLICATE_MESSAGE} header to {@code true}.
 * This header can be used in the downstream business logic to achieve "idempotency".
 * <p>
 * Internally {@link IdempotentReceiver} is based on the {@link MetadataStore}
 * and stores {@code idempotentKey : messageId} pairs of accepted messages.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class IdempotentReceiver extends MessageFilter {

	private final MetadataStore store;

	private final IdempotentKeyStrategy idempotentKeyStrategy;

	private volatile boolean filter = true;

	public IdempotentReceiver(IdempotentKeyStrategy idempotentKeyStrategy) {
		this(idempotentKeyStrategy, new SimpleMetadataStore());
	}

	public IdempotentReceiver(final IdempotentKeyStrategy idempotentKeyStrategy, final MetadataStore store) {
		super(new MessageSelector() {

			@Override
			public boolean accept(Message<?> message) {
				return store.get(idempotentKeyStrategy.getIdempotentKey(message)) == null;
			}

		});
		Assert.notNull(store, "'store' can't be null");
		Assert.notNull(idempotentKeyStrategy, "'idempotentKeyStrategy' can't be null");
		this.idempotentKeyStrategy = idempotentKeyStrategy;
		this.store = store;
	}

	public void setFilter(boolean filter) {
		this.filter = filter;
	}

	@Override
	protected Object doHandleRequestMessage(Message<?> message) {
		Object reply = super.doHandleRequestMessage(message);
		if (reply == null) {
			return this.filter ? null : getMessageBuilderFactory().fromMessage(message)
					.setHeader(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, true);
		}
		else {
			this.store.put(idempotentKeyStrategy.getIdempotentKey(message), message.getHeaders().getId().toString());
			return message;
		}
	}

}
