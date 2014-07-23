/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Collection;

import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.messaging.Message;

/**
 * Aggregator specific implementation of {@link AbstractCorrelatingMessageHandler}.
 * Will remove {@link MessageGroup}s in the {@linkplain #afterRelease}
 * only if 'expireGroupsUponCompletion' flag is set to 'true'.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.1
 */
public class AggregatingMessageHandler extends AbstractCorrelatingMessageHandler {

	private volatile boolean expireGroupsUponCompletion = false;

	public AggregatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store,
			CorrelationStrategy correlationStrategy, ReleaseStrategy releaseStrategy) {
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
	 *
	 * @param expireGroupsUponCompletion true when groups should be expired on completion.
	 *
	 * @see #afterRelease
	 */
	public void setExpireGroupsUponCompletion(boolean expireGroupsUponCompletion) {
		this.expireGroupsUponCompletion = expireGroupsUponCompletion;
	}

	@Override
	public void setExpireGroupsUponTimeout(boolean expireGroupsOnTimeout) {
		super.setExpireGroupsUponTimeout(expireGroupsOnTimeout);
	}

	@Override
	protected void afterRelease(MessageGroup messageGroup, Collection<Message<?>> completedMessages) {
		this.messageStore.completeGroup(messageGroup.getGroupId());

		if (this.expireGroupsUponCompletion) {
			remove(messageGroup);
		}
		else {
			for (Message<?> message : messageGroup.getMessages()) {
				this.messageStore.removeMessageFromGroup(messageGroup.getGroupId(), message);
			}
		}
	}

}
