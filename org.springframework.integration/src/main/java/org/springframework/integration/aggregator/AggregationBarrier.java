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

package org.springframework.integration.aggregator;

import java.util.List;

import org.springframework.integration.core.Message;

/**
 * MessageBarrier implementation for message aggregation. Delegates to a
 * {@link CompletionStrategy} to determine when the group of messages is ready
 * for aggregation.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class AggregationBarrier extends AbstractMessageBarrier {

	protected final CompletionStrategy completionStrategy;


	public AggregationBarrier(CompletionStrategy completionStrategy) {
		this.completionStrategy = completionStrategy;
	}


	protected List<Message<?>> releaseAvailableMessages() {
		return (this.isComplete()) ? this.getMessages() : null;
	}

	protected boolean hasReceivedAllMessages() {
		return completionStrategy.isComplete(this.messages);
	}
}
