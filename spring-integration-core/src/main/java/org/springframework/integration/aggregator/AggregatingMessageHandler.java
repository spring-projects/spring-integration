/*
 * Copyright 2002-2013 the original author or authors.
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
import java.util.Date;
import java.util.Iterator;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.Message;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;

/**
 * Aggregator specific implementation of {@link AbstractCorrelatingMessageHandler}.
 * Will remove {@link MessageGroup}s only if {@linkplain #expireGroupsUponCompletion} flag is set to <code>true</code>.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.1
 */
public class AggregatingMessageHandler extends AbstractCorrelatingMessageHandler
		implements ApplicationListener<ContextRefreshedEvent> {

	private volatile boolean initialized = false;

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
	 * Will set the {@linkplain #expireGroupsUponCompletion} flag and if it is
	 * set to <code>true</code> and if {@linkplain #initialized} is set to <code>true</code> too,
	 * it will also remove all 'complete' {@link MessageGroup}s
	 *
	 * @param expireGroupsUponCompletion
	 *
	 * @see #removeCompleteMessageGroups
	 */
	public void setExpireGroupsUponCompletion(boolean expireGroupsUponCompletion) {
		this.expireGroupsUponCompletion = expireGroupsUponCompletion;
		if (expireGroupsUponCompletion && this.initialized) {
			this.removeCompleteMessageGroups();
		}
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		this.initialized = true;
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

	private void removeCompleteMessageGroups() {
		Iterator<MessageGroup> messageGroups = this.messageStore.iterator();
		while (messageGroups.hasNext()) {
			MessageGroup messageGroup = messageGroups.next();
			if (messageGroup.isComplete()) {
				this.remove(messageGroup);
			}
		}

	}

	/**
	 * Handles {@link ContextRefreshedEvent} to schedules the task for {@linkplain #removeCompleteMessageGroups}
	 * as late as possible after application context startup.
	 * Also it checks equality of event's <code>applicationContext</code> and
	 * provided <code>beanFactory</code> to ignore other {@link ContextRefreshedEvent}s
	 * which may be published in the 'parent-child' contexts, e.g. in the Spring-MVC applications.
	 * This behavior is dictated by the avoidance of invocation thread overload,
	 * especially when the provided {@link MessageGroupStore} is persistent.
	 *
	 * @param event - {@link ContextRefreshedEvent} which occurs
	 *              after Application context is completely initialized.
	 *
	 * @see #removeCompleteMessageGroups
	 */
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext().equals(this.getBeanFactory())) {
			if (this.expireGroupsUponCompletion) {
				this.getTaskScheduler().schedule(new Runnable() {
					public void run() {
						AggregatingMessageHandler.this.removeCompleteMessageGroups();
					}
				}, new Date());
			}
		}
	}

}
