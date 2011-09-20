/*
 * Copyright 2002-2011 the original author or authors.
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
import java.util.Iterator;

import org.springframework.integration.Message;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;

/**
 * Aggregator specific implementation of {@link AbstractCorrelatingMessageHandler}. 
 * Will remove {@link MessageGroup}s only if 'expireGroupsUponCompletion' flag is set to 'true'.
 *
 * @author Oleg Zhurakousky
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
	 * Will set the 'expireGroupsUponCompletion' flag and if it is 
	 * set to 'true' it will also remove all 'complete' {@link MessageGroup}s
	 * @param expireGroupsUponCompletion
	 */
	public void setExpireGroupsUponCompletion(boolean expireGroupsUponCompletion) {
		this.expireGroupsUponCompletion = expireGroupsUponCompletion;
		if (expireGroupsUponCompletion) {
			Iterator<MessageGroup> messageGroups = this.messageStore.iterator();
			while (messageGroups.hasNext()) {
				MessageGroup messageGroup = messageGroups.next();
				if (messageGroup.isComplete()) {
					remove(messageGroup);
				}
			}
		}
	}

	@Override
	protected void afterRelease(MessageGroup messageGroup, Collection<Message<?>> completedMessages) {
		this.messageStore.completeGroup(messageGroup.getGroupId());
		
		if (this.expireGroupsUponCompletion) {
			remove(messageGroup);
		}
		else {
			if (this.keepReleasedMessages){
				messageStore.markMessageGroup(messageGroup);
			}
			else {
				for (Message<?> message : messageGroup.getMarked()) {
					this.messageStore.removeMessageFromGroup(messageGroup.getGroupId(), message);
				}
				for (Message<?> message : messageGroup.getUnmarked()) {
					this.messageStore.removeMessageFromGroup(messageGroup.getGroupId(), message);
				}
			}
		}	
	}

}
