/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.messaging.Message;

/**
 * Aggregator specific implementation of {@link AbstractCorrelatingMessageHandler}.
 * Will remove {@link MessageGroup}s in the {@linkplain #afterRelease}
 * only if 'expireGroupsUponCompletion' flag is set to 'true'.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Ngoc Nhan
 *
 * @since 2.1
 */
public class AggregatingMessageHandler extends AbstractCorrelatingMessageHandler {

	private volatile boolean expireGroupsUponCompletion = false;

	public AggregatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store,
			@Nullable CorrelationStrategy correlationStrategy, @Nullable ReleaseStrategy releaseStrategy) {

		super(processor, store, correlationStrategy, releaseStrategy);
	}

	public AggregatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store) {
		super(processor, store);
	}

	public AggregatingMessageHandler(MessageGroupProcessor processor) {
		super(processor);
	}

	/**
	 * Will set the 'expireGroupsUponCompletion' flag.
	 * @param expireGroupsUponCompletion true when groups should be expired on completion.
	 * @see #afterRelease
	 */
	public void setExpireGroupsUponCompletion(boolean expireGroupsUponCompletion) {
		this.expireGroupsUponCompletion = expireGroupsUponCompletion;
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.aggregator;
	}

	@Override
	protected boolean isExpireGroupsUponCompletion() {
		return this.expireGroupsUponCompletion;
	}

	/**
	 * Check an {@link Iterable} result for split possibility on the output production:
	 * the items of the collection have to be instances of {@link Message}
	 * or {@link org.springframework.integration.support.AbstractIntegrationMessageBuilder}
	 * and {@link #getOutputProcessor()} has to be a {@link SimpleMessageGroupProcessor}.
	 * Otherwise, a single reply message is emitted with the whole {@link Iterable} as its payload.
	 * @param reply the {@link Iterable} result to check for split possibility.
	 * @return true if the {@link Iterable} result has to be split into individual messages.
	 * @since 6.0
	 */
	@Override
	protected boolean shouldSplitOutput(Iterable<?> reply) {
		return getOutputProcessor() instanceof SimpleMessageGroupProcessor && super.shouldSplitOutput(reply);
	}

	/**
	 * Complete the group and remove all its messages.
	 * If the {@link #expireGroupsUponCompletion} is true, then remove group fully.
	 * @param messageGroup the group to clean up.
	 * @param completedMessages The completed messages. Ignored in this implementation.
	 */
	@Override
	protected void afterRelease(MessageGroup messageGroup, @Nullable Collection<Message<?>> completedMessages) {
		Object groupId = messageGroup.getGroupId();
		MessageGroupStore messageStore = getMessageStore();
		messageStore.completeGroup(groupId);

		if (this.expireGroupsUponCompletion) {
			remove(messageGroup);
		}
		else {
			if (messageStore instanceof SimpleMessageStore simpleMessageStore) {
				simpleMessageStore.clearMessageGroup(groupId);
			}
			else {
				messageStore.removeMessagesFromGroup(groupId, messageGroup.getMessages());
			}
		}
	}

}
