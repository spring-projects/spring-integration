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
package org.springframework.integration.store;

import java.util.Iterator;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;

/**
 * Defines additional storage operations on groups of messages linked by a group id.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 * @since 2.0
 *
 */
public interface MessageGroupStore extends BasicMessageGroupStore {

	/**
	 * Optional attribute giving the number of messages in the store over all groups. Implementations may decline to
	 * respond by throwing an exception.
	 *
	 * @return the number of messages
	 * @throws UnsupportedOperationException if not implemented
	 */
	@ManagedAttribute
	int getMessageCountForAllMessageGroups();

	/**
	 * Optional attribute giving the number of  message groups. Implementations may decline
	 * to respond by throwing an exception.
	 *
	 * @return the number message groups
	 * @throws UnsupportedOperationException if not implemented
	 */
	@ManagedAttribute
	int getMessageGroupCount();

	/**
	 * Persist a deletion on a single message from the group. The group is modified to reflect that 'messageToRemove' is
	 * no longer present in the group.
	 *
	 * @param key The groupId for the group containing the message.
	 * @param messageToRemove The message to be removed.
	 * @return The message Group.
	 */
	MessageGroup removeMessageFromGroup(Object key, Message<?> messageToRemove);

	/**
	 * Register a callback for when a message group is expired through {@link #expireMessageGroups(long)}.
	 *
	 * @param callback A callback to execute when a message group is cleaned up.
	 */
	void registerMessageGroupExpiryCallback(MessageGroupCallback callback);

	/**
	 * Extract all expired groups (whose timestamp is older than the current time less the threshold provided) and call
	 * each of the registered callbacks on them in turn. For example: call with a timeout of 100 to expire all groups
	 * that were created more than 100 milliseconds ago, and are not yet complete. Use a timeout of 0 (or negative to be
	 * on the safe side) to expire all message groups.
	 *
	 * @param timeout the timeout threshold to use
	 * @return the number of message groups expired
	 *
	 * @see #registerMessageGroupExpiryCallback(MessageGroupCallback)
	 */
	int expireMessageGroups(long timeout);

	/**
	 * Allows you to set the sequence number of the last released Message. Used for Resequencing use cases
	 *
	 * @param groupId The group identifier.
	 * @param sequenceNumber The sequence number.
	 */
	void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber);

	/**
	 * @return The iterator of currently accumulated {@link MessageGroup}s.
	 */
	Iterator<MessageGroup> iterator();

	/**
	 * Completes this MessageGroup. Completion of the MessageGroup generally means
	 * that this group should not be allowing any more mutating operation to be performed on it.
	 * For example any attempt to add/remove new Message form the group should not be allowed.
	 *
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
	 * Return the one {@link org.springframework.messaging.Message} from {@link org.springframework.integration.store.MessageGroup}.
	 * @param groupId The group identifier.
	 * @return the {@link org.springframework.messaging.Message}.
	 * @since 4.0
	 */
	Message<?> getOneMessageFromGroup(Object groupId);

	/**
	 * Invoked when a MessageGroupStore expires a group.
	 */
	public interface MessageGroupCallback {

		void execute(MessageGroupStore messageGroupStore, MessageGroup group);

	}

}
