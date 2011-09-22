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

import org.springframework.integration.Message;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;

/**
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class ResequensingMessageHandler extends CorrelatingMessageHandler {

	/**
	 * @param processor
	 * @param store
	 * @param correlationStrategy
	 * @param releaseStrategy
	 */
	public ResequensingMessageHandler(MessageGroupProcessor processor,
			MessageGroupStore store, CorrelationStrategy correlationStrategy,
			ReleaseStrategy releaseStrategy) {
		super(processor, store, correlationStrategy, releaseStrategy);
	}

	/**
	 * @param processor
	 * @param store
	 */
	public ResequensingMessageHandler(MessageGroupProcessor processor,
			MessageGroupStore store) {
		super(processor, store);
	}

	/**
	 * @param processor
	 */
	public ResequensingMessageHandler(MessageGroupProcessor processor) {
		super(processor);
	}
	
	@SuppressWarnings("rawtypes")
	protected void cleanUpForReleasedGroup(MessageGroup group, Collection<Message> completedMessages) {
		if (completedMessages == null) {
			mark(group);
		} else {
			mark(group, completedMessages);
		}
	}

}
