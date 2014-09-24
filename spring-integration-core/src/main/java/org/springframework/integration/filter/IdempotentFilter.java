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

import java.util.Collection;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.util.FunctionIterator;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import reactor.function.Function;

/**
 * The {@link MessageFilter} implementation for the
 * <a href="http://www.eaipatterns.com/IdempotentReceiver.html">Idempotent Receiver</a>
 * EIP functional pattern.
 * <p>
 * By default it works like a generic {@link MessageFilter}: if message isn't accepted
 * it discarded to the {@code discardChannel} or
 * a {@link org.springframework.integration.MessageRejectedException} is thrown.
 * However with {@code skipDuplicate = false} the message is sent as normal, but with
 * {@link IntegrationMessageHeaderAccessor#DUPLICATE_MESSAGE} header to {@code true}.
 * This header can be used in the downstream business logic to achieve "idempotency".
 * <p>
 * Internally {@link IdempotentFilter} is based on the {@link MessageGroupStore}
 * and stores all accepted messages to the {@code messageGroup}
 * under provided {@link #groupId}.
 * <p>
 * One of provided {@link IdempotentKeyStrategy} or {@link IdempotentSelector} is used to determine
 * if message is already contained in the {@link MessageGroupStore}.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class IdempotentFilter extends MessageFilter {

	private final String groupId;

	private final MessageGroupStore store;

	private volatile boolean skipDuplicate = true;

	public IdempotentFilter(String groupId, IdempotentKeyStrategy idempotentKeyStrategy) {
		this(groupId, idempotentKeyStrategy, new SimpleMessageStore(1));
	}

	public IdempotentFilter(String groupId, IdempotentKeyStrategy idempotentKeyStrategy, MessageGroupStore store) {
		this(groupId, idempotentKeyStrategy, null, store);
	}

	public IdempotentFilter(String groupId, IdempotentSelector idempotentSelector) {
		this(groupId, idempotentSelector, new SimpleMessageStore(1));
	}

	public IdempotentFilter(String groupId, IdempotentSelector idempotentSelector, MessageGroupStore store) {
		this(groupId, null, idempotentSelector, store);
	}

	private IdempotentFilter(String groupId, IdempotentKeyStrategy idempotentKeyStrategy,
			IdempotentSelector idempotentSelector, MessageGroupStore store) {
		super(new MessageSelector() {

			IdempotentSelector selector = (idempotentSelector != null
					? idempotentSelector
					: new IdempotentSelector() {

				@Override
				public boolean accept(Collection<Message<?>> messages, Message<?> message) {
					return !CollectionUtils.contains(
							new FunctionIterator<Message<?>, Object>(store.getMessageGroup(groupId).getMessages(),
									new Function<Message<?>, Object>() {

										@Override
										public Object apply(Message<?> message) {
											return idempotentKeyStrategy.getIdempotentKey(message);
										}

									}), idempotentKeyStrategy.getIdempotentKey(message));

				}

			});

			@Override
			public boolean accept(Message<?> message) {
				return this.selector.accept(store.getMessageGroup(groupId).getMessages(), message);
			}

		});
		Assert.hasText(groupId, "'groupId' must not be empty");
		Assert.notNull(store, "'store' can't be null");
		Assert.state(idempotentSelector != null || idempotentKeyStrategy != null,
				"One of 'idempotentSelector' or 'idempotentKeyStrategy' must be provided");
		this.groupId = groupId;
		this.store = store;
	}

	public void setSkipDuplicate(boolean skipDuplicate) {
		this.skipDuplicate = skipDuplicate;
	}

	@Override
	protected Object doHandleRequestMessage(Message<?> message) {
		Object reply = super.doHandleRequestMessage(message);
		if (reply == null) {
			return this.skipDuplicate ? null : getMessageBuilderFactory().fromMessage(message)
					.setHeader(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, true);
		}
		else {
			this.store.addMessageToGroup(this.groupId, message);
			return message;
		}
	}

}
