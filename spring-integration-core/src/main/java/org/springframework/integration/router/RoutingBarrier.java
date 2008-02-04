/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.router;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * A rendezvous point for {@link Message Messages} that delegates to a
 * {@link RoutingBarrierCompletionStrategy} to determine when a
 * <em>complete</em> message group is available.
 * 
 * @author Mark Fisher
 */
public class RoutingBarrier {

	private List<Message<?>> messages = new CopyOnWriteArrayList<Message<?>>();

	private final RoutingBarrierCompletionStrategy completionStrategy;

	private volatile boolean complete = false;

	private ReentrantLock lock = new ReentrantLock();

	private Condition condition = lock.newCondition();


	public RoutingBarrier(RoutingBarrierCompletionStrategy completionStrategy) {
		Assert.notNull(completionStrategy, "'completionStrategy' must not be null");
		this.completionStrategy = completionStrategy;
	}


	public void addMessage(Message<?> message) {
		this.messages.add(message);
		if (this.completionStrategy.isComplete(this.messages)) {
			try {
				this.lock.lock();
				if (!this.complete) {
					this.complete = true;
					this.condition.signalAll();
				}
			}
			finally {
				this.lock.unlock();
			}
		}
	}

	public boolean waitForCompletion(long timeout) {
		if (this.complete) {
			return true;
		}
		lock.lock();
		try {
			if (this.complete) {
				return true;
			}
			if (timeout >= 0) {
				return this.condition.await(timeout, TimeUnit.MILLISECONDS);
			}
			else {
				this.condition.await();
				return true;
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
		finally {
			lock.unlock();
		}
	}

	public boolean isComplete() {
		return this.complete;
	}

	public List<Message<?>> getMessages() {
		return this.messages;
	}

}
