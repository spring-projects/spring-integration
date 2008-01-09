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

package org.springframework.integration.bus;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * Abstract base class for message dispatchers. Delegates to a
 * {@link MessageRetriever} strategy.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessageDispatcher implements MessageDispatcher {

	protected Log logger = LogFactory.getLog(this.getClass());

	private MessageRetriever retriever;

	private List<MessageReceivingExecutor> executors = new CopyOnWriteArrayList<MessageReceivingExecutor>();


	public AbstractMessageDispatcher(MessageRetriever retriever) {
		this.retriever = retriever;
	}


	public void addExecutor(MessageReceivingExecutor executor) {
		Assert.notNull(executor, "'executor' must not be null");
		executor.start();
		this.executors.add(executor);
	}

	protected List<MessageReceivingExecutor> getExecutors() {
		return this.executors;
	}

	/**
	 * Retrieves messages and dispatches to the executors.
	 * 
	 * @return the number of messages processed
	 */
	public int dispatch() {
		int messagesProcessed = 0;
		Collection<Message<?>> messages = this.retriever.retrieveMessages();
		if (messages == null) {
			return 0;
		}
		for (Message<?> message : messages) {
			if (dispatchMessage(message)) {
				messagesProcessed++;
			}
		}
		return messagesProcessed;
	}


	protected abstract boolean dispatchMessage(Message<?> message);

}
