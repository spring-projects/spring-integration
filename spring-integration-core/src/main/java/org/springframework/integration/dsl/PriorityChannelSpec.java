/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.Comparator;

import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.store.MessageGroupQueue;
import org.springframework.integration.store.PriorityCapableChannelMessageStore;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class PriorityChannelSpec extends MessageChannelSpec<PriorityChannelSpec, PriorityChannel> {

	private int capacity;

	private Comparator<Message<?>> comparator;

	private MessageGroupQueue messageGroupQueue;

	protected PriorityChannelSpec() {
	}

	public PriorityChannelSpec capacity(int capacity) {
		this.capacity = capacity;
		return this;
	}

	public PriorityChannelSpec comparator(Comparator<Message<?>> comparator) {
		this.comparator = comparator;
		return this;
	}

	public PriorityChannelSpec messageStore(PriorityCapableChannelMessageStore messageGroupStore, Object groupId) {
		this.messageGroupQueue = new MessageGroupQueue(messageGroupStore, groupId);
		this.messageGroupQueue.setPriority(true);
		return this;
	}

	@Override
	protected PriorityChannel doGet() {
		Assert.state(!(this.comparator != null && this.messageGroupQueue != null),
				"Only one of 'comparator' or 'messageGroupStore' can be specified.");

		if (this.messageGroupQueue != null) {
			this.channel = new PriorityChannel(this.messageGroupQueue);
		}
		else {
			this.channel = new PriorityChannel(this.capacity, this.comparator);
		}

		return super.doGet();
	}

}
