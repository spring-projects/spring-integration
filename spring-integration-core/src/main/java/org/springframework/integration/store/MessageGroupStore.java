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
import java.util.Iterator;
import java.util.stream.Stream;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.Message;

/**
 * Defines additional storage operations on groups of messages linked by a group id.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public interface MessageGroupStore extends BasicMessageGroupStore {

	/**
	 * Optional attribute giving the number of messages in the store over all groups.
	 * Implementations may decline to respond by throwing an exception.
	 * @return the number of messages
	 * @throws UnsupportedOperationException if not implemented
	 */
	@ManagedAttribute
	int getMessageCountForAllMessageGroups();

	/**
	 * Optional attribute giving the number of  message groups.
	 * Implementations may decline to respond by throwing an exception.
	 * @return the number message groups
	 * @throws UnsupportedOperationException if not implemented
	 */
	@ManagedAttribute
	int getMessageGroupCount();

	/**
	 * Persist the deletion of messages from the group.
	 * @param key The groupId for the group containing the message(s).
	 * @param messages The messages to be removed.
	 * @since 4.2
	 */
	void removeMessagesFromGroup(Object key, Collection<Message<?>> messages);

	/**
	 * Persist the deletion of messages from the group.
	 * @param key The groupId for the group containing the message(s).
	 * @param messages The messages to be removed.
	 * @since 4.2
	 */
	void removeMessagesFromGroup(Object key, Message<?>... messages);

	/**
	 * Register a callback for when a message group is expired through {@link #expireMessageGroups(long)}.
	 * @param callback A callback to execute when a message group is cleaned up.
	 */
	void registerMessageGroupExpiryCallback(MessageGroupCallback callback);

	/**
	 * Extract all expired groups (whose timestamp is older than the current time less the threshold provided) and call
	 * each of the registered callbacks on them in turn. For example: call with a timeout of 100 to expire all groups
	 * that were created more than 100 milliseconds ago, and are not yet complete. Use a timeout of 0 (or negative to be
	 * on the safe side) to expire all message groups.
	 * @param timeout the timeout threshold to use
	 * @return the number of message groups expired
	 * @see #registerMessageGroupExpiryCallback(MessageGroupCallback)
	 */
	@ManagedOperation
	int expireMessageGroups(long timeout);

	/**
	 * Allows you to set the sequence number of the last released Message. Used for Resequencing use cases
	 * @param groupId The group identifier.
	 * @param sequenceNumber The sequence number.
	 */
	void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber);

	/**
	 * Add a condition sentence into the group.
	 * Can be used later on for making some decisions for group, e.g. release strategy
	 * for correlation handler can consult this condition instead of iterating all
	 * the messages in group.
	 * @param groupId The group identifier.
	 * @param condition The condition to store into the group.
	 * @since 5.5
	 */
	void setGroupCondition(Object groupId, String condition);

	/**
	 * @return The iterator of currently accumulated {@link MessageGroup}s.
	 */
	Iterator<MessageGroup> iterator();

	/**
	 * Completes this MessageGroup. Completion of the MessageGroup generally means
	 * that this group should not be allowing any more mutating operation to be performed on it.
	 * For example any attempt to add/remove new Message form the group should not be allowed.
	 * @param groupId The group identifier.
	 */
	void completeGroup(Object groupId);

	/**
	 * Obtain the group metadata without fetching any messages; must supply all other
	 * group properties; may include the id of the first message.
	 * @param groupId The group id.
	 * @return The metadata.
	 * @since 4.0
	 */
	MessageGroupMetadata getGroupMetadata(Object groupId);

	/**
	 * Return the one {@link Message} from {@link MessageGroup}.
	 * @param groupId The group identifier.
	 * @return the {@link Message}.
	 * @since 4.0
	 */
	Message<?> getOneMessageFromGroup(Object groupId);

	/**
	 * Store messages with an association to a group id.
	 * This can be used to group messages together.
	 * @param groupId The group id to store messages under.
	 * @param messages The messages to add.
	 * @since 4.3
	 */
	void addMessagesToGroup(Object groupId, Message<?>... messages);

	/**
	 * Retrieve messages for the provided group id.
	 * @param groupId The group id to retrieve messages for.
	 * @return the messages for group.
	 * @since 4.3
	 */
	Collection<Message<?>> getMessagesForGroup(Object groupId);

	/**
	 * Return a stream for messages stored in the provided group.
	 * @param groupId the group id to retrieve messages.
	 * @return the {@link Stream} for messages in this group.
	 * @since 5.5
	 */
	default Stream<Message<?>> streamMessagesForGroup(Object groupId) {
		return getMessagesForGroup(groupId).stream();
	}

	/**
	 * Invoked when a MessageGroupStore expires a group.
	 */
	@FunctionalInterface
	interface MessageGroupCallback {

		void execute(MessageGroupStore messageGroupStore, MessageGroup group);

	}

}
