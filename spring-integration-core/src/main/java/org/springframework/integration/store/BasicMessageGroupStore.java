/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.store;

import java.util.Collection;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;

/**
 * Defines a minimal message group store with basic capabilities.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Fan Kongyuan
 *
 * @since 4.0
 *
 */
public interface BasicMessageGroupStore {

	/**
	 * Return the size of this MessageGroup.
	 * @param groupId The group identifier.
	 * @return The size.
	 */
	@ManagedAttribute
	int messageGroupSize(Object groupId);

	/**
	 * Return all Messages currently in the MessageStore that were stored using
	 * {@link #addMessageToGroup(Object, Message)} with this group id.
	 * @param groupId The group identifier.
	 * @return A group of messages, empty if none exists for this key.
	 */
	MessageGroup getMessageGroup(Object groupId);

	/**
	 * Retrieve messages for the provided group id without removing them from the store.
	 * By default, obtains the messages from {@link #getMessageGroup(Object)}.
	 * @param groupId the group id to retrieve messages for.
	 * @return the messages for the group, empty if none exists.
	 * @since 7.3
	 */
	default Collection<Message<?>> getMessagesForGroup(Object groupId) {
		return getMessageGroup(groupId).getMessages();
	}

	/**
	 * Return a stream for messages stored in the provided group without removing them.
	 * The persistent implementations may return a stream which has to be closed once
	 * processed (e.g. through a try-with-resources clause).
	 * By default, streams the result of {@link #getMessagesForGroup(Object)}.
	 * @param groupId the group id to retrieve messages for.
	 * @return the stream for messages in this group.
	 * @since 7.3
	 */
	default Stream<Message<?>> streamMessagesForGroup(Object groupId) {
		return getMessagesForGroup(groupId).stream();
	}

	/**
	 * Store a message with an association to a group id. This can be used to group messages together.
	 * @param groupId The group id to store the message under.
	 * @param message A message.
	 * @return The message group.
	 */
	MessageGroup addMessageToGroup(Object groupId, Message<?> message);

	/**
	 * Poll Message from this {@link MessageGroup} (in FIFO style if supported by the implementation)
	 * while also removing the polled {@link Message}.
	 * @param groupId The group identifier.
	 * @return The message.
	 */
	@Nullable
	Message<?> pollMessageFromGroup(Object groupId);

	/**
	 * Remove the message group with this id.
	 * @param groupId The id of the group to remove.
	 */
	void removeMessageGroup(Object groupId);

}
