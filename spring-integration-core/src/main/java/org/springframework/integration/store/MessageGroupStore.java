/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.integration.Message;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * Interface for storage operations on groups of messages linked by a group id.
 * 
 * @author Dave Syer
 * 
 * @since 2.0
 * 
 */
public interface MessageGroupStore {

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
	 * Optional attribute giving the number of marked messages in the store for all groups. Implementations may decline
	 * to respond by throwing an exception.
	 * 
	 * @return the number of marked messages in each group
	 * @throws UnsupportedOperationException if not implemented
	 */
	@ManagedAttribute
	int getMarkedMessageCountForAllMessageGroups();

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
	 * Return all Messages currently in the MessageStore that were stored using
	 * {@link #addMessageToGroup(Object, Message)} with this group id.
	 * 
	 * @return a group of messages, empty if none exists for this key
	 */
	MessageGroup getMessageGroup(Object groupId);

	/**
	 * Store a message with an association to a group id. This can be used to group messages together.
	 * 
	 * @param groupId the group id to store the message under
	 * @param message a message
	 */
	MessageGroup addMessageToGroup(Object groupId, Message<?> message);

	/**
	 * Persist the mark on all the messages from the group. The group is modified in the process as all its unmarked
	 * messages become marked.
	 * 
	 * @param group a MessageGroup with no unmarked messages
	 */
	MessageGroup markMessageGroup(MessageGroup group);

	/**
	 * Persist a deletion on a single message from the group. The group is modified to reflect that 'messageToRemove' is
	 * no longer present in the group.
	 * @param key the groupId for the group containing the message
	 * @param messageToRemove the message to be removed
	 */
	MessageGroup removeMessageFromGroup(Object key, Message<?> messageToRemove);

	/**
	 * Persist a mark on a single message from the group. The group is modified to reflect that 'messageToMark' is no
	 * longer unmarked but became marked instead.
	 * @param key the groupId for the group containing the message
	 * @param messageToMark the message to be marked
	 */
	MessageGroup markMessageFromGroup(Object key, Message<?> messageToMark);

	/**
	 * Remove the message group with this id.
	 * 
	 * @param groupId the id of the group to remove
	 */
	void removeMessageGroup(Object groupId);

	/**
	 * Register a callback for when a message group is expired through {@link #expireMessageGroups(long)}.
	 * 
	 * @param callback a callback to execute when a message group is cleaned up
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
	 * Returns the iterator of currently accumulated {@link MessageGroup}s
	 */
	Iterator<MessageGroup> iterator();
}