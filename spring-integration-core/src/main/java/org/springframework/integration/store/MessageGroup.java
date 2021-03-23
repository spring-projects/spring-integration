/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.store;

import java.util.Collection;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * A group of messages that are correlated with each other and should be processed in the same context.
 * <p>
 * The message group allows implementations to be mutable, but this behavior is optional.
 * Implementations should take care to document their thread safety and mutability.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public interface MessageGroup {

	/**
	 * Query if the message can be added.
	 * @param message The message.
	 * @return true if the message can be added.
	 */
	boolean canAdd(Message<?> message);

	/**
	 * Add the message to this group.
	 * @param messageToAdd the message to add.
	 * @since 4.3
	 */
	void add(Message<?> messageToAdd);

	/**
	 * Remove the message from this group.
	 * @param messageToRemove the message to remove.
	 * @return {@code true} if a message was removed.
	 * @since 4.3
	 */
	boolean remove(Message<?> messageToRemove);

	/**
	 * Return all available Messages from the group at the time of invocation
	 * @return The messages.
	 */
	Collection<Message<?>> getMessages();

	/**
	 * Return a stream for messages stored in this group.
	 * @return the {@link Stream} for messages in this group.
	 * @since 5.5
	 */
	default Stream<Message<?>> streamMessages() {
		return getMessages().stream();
	}

	/**
	 * @return the key that links these messages together
	 */
	Object getGroupId();

	/**
	 * @return the sequenceNumber of the last released message. Used in Resequencer use cases only
	 */
	int getLastReleasedMessageSequenceNumber();

	void setLastReleasedMessageSequenceNumber(int sequenceNumber);

	/**
	 * @return true if the group is complete (i.e. no more messages are expected to be added)
	 */
	boolean isComplete();

	/**
	 * Complete the group.
	 */
	void complete();

	/**
	 * @return the size of the sequence expected 0 if unknown
	 */
	int getSequenceSize();

	/**
	 * @return the total number of messages in this group
	 */
	int size();

	/**
	 * @return a single message from the group
	 */
	Message<?> getOne();

	/**
	 * @return the timestamp (milliseconds since epoch) associated with the creation of this group
	 */
	long getTimestamp();

	/**
	 * @return the timestamp (milliseconds since epoch) associated with the time this group was last updated
	 */
	long getLastModified();

	void setLastModified(long lastModified);

	/**
	 * Add a condition statement to this group which can be consulted later on, e.g. from the release strategy.
	 * @param condition statement which could be consulted later on, e.g. from the release strategy.
	 * @since 5.5
	 */
	void setCondition(String condition);

	/**
	 * Return the condition for this group to consult with, e.g. from the release strategy.
	 * @return the condition for this group to consult with, e.g. from the release strategy.
	 * @since 5.5
	 */
	@Nullable
	String getCondition();

	void clear();

}
