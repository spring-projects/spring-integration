/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.dispatcher;

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageConsumer;

/**
 * A broadcasting dispatcher implementation. It makes a best effort to
 * send the message to each of its endpoints. If it fails to send to any
 * one endpoints, it will log a warn-level message but continue to send
 * to the other endpoints.
 * 
 * @author Mark Fisher
 */
public class BroadcastingDispatcher extends AbstractDispatcher {

	private volatile boolean applySequence;


	/**
	 * Specify whether to apply sequence numbers to the messages
	 * prior to sending to the endpoints. By default, sequence
	 * numbers will <em>not</em> be applied
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
	}

	public boolean dispatch(Message<?> message) {
		int sequenceNumber = 1;
		int sequenceSize = this.subscribers.size();
		for (final MessageConsumer consumer : this.subscribers) {
			final Message<?> messageToSend = (!this.applySequence) ? message
				: MessageBuilder.fromMessage(message)
						.setSequenceNumber(sequenceNumber++)
						.setSequenceSize(sequenceSize)
						.build();
			TaskExecutor executor = this.getTaskExecutor();
			if (executor != null) {
				executor.execute(new Runnable() {
					public void run() {
						BroadcastingDispatcher.this.sendMessageToConsumer(messageToSend, consumer);
					}
				});
			}
			else {
				this.sendMessageToConsumer(messageToSend, consumer);
			}
		}
		return true;
	}

}
