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

import org.springframework.integration.store.MessageGroup;

/**
 * A processor for <i>correlated</i> groups of messages.
 *
 * @author Iwein Fuld
 * @see org.springframework.integration.aggregator.AbstractCorrelatingMessageHandler
 */
public interface MessageGroupProcessor {

	/**
	 * Process the given MessageGroup. Implementations are free to return as few or as many messages based on the
	 * invocation as needed. For example an aggregating processor will return only a single message representing the
	 * group, while a resequencing processor will return all messages whose preceding sequence has been satisfied.
	 * <p>
	 * If a multiple messages are returned the return value must be a Collection&lt;Message&gt;.
	 *
	 * @param group The message group.
	 * @return The result of processing the group.
	 */
	Object processMessageGroup(MessageGroup group);

}
