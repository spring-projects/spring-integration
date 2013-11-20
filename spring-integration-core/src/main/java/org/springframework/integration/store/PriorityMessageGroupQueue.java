/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.store;

import java.util.concurrent.locks.Lock;

import org.springframework.integration.Message;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class PriorityMessageGroupQueue extends MessageGroupQueue {

	public PriorityMessageGroupQueue(MessageGroupStore messageGroupStore, Object groupId) {
		super(messageGroupStore, groupId);
	}

	public PriorityMessageGroupQueue(MessageGroupStore messageGroupStore, Object groupId, int capacity) {
		super(messageGroupStore, groupId, capacity);
	}

	public PriorityMessageGroupQueue(MessageGroupStore messageGroupStore, Object groupId, Lock storeLock) {
		super(messageGroupStore, groupId, storeLock);
	}

	public PriorityMessageGroupQueue(MessageGroupStore messageGroupStore, Object groupId, int capacity, Lock storeLock) {
		super(messageGroupStore, groupId, capacity, storeLock);
	}

	@Override
	protected Message<?> doPoll() {
		Message<?> message = this.messageGroupStore.pollMessageFromGroupByPriority(this.groupId);
		this.messageStoreNotFull.signal();
		return message;
	}

}
