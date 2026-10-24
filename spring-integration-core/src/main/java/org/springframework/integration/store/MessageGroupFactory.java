/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.store;

import java.util.Collection;

import org.springframework.messaging.Message;

/**
 * The {@link MessageGroup} factory strategy.
 * This strategy is used from the {@link MessageGroup}-aware components, e.g. {@code MessageGroupStore}.
 *
 * @author Artem Bilan
 * @author 樊孔缘
 * @since 4.3
 */
public interface MessageGroupFactory {

	/**
	 * Create a {@link MessageGroup} instance based on the provided {@code groupId}.
	 * @param groupId the group id to use.
	 * @return the {@link MessageGroup} instance.
	 */
	MessageGroup create(Object groupId);

	/**
	 * Create a {@link MessageGroup} instance based on the provided {@code groupId}
	 * and with the {@code messages} for the group.
	 * @param messages the messages for the group.
	 * @param groupId the group id to use.
	 * @return the {@link MessageGroup} instance.
	 */
	MessageGroup create(Collection<? extends Message<?>> messages, Object groupId);

	/**
	 * Create a {@link MessageGroup} instance based on the provided {@code groupId}
	 * and with the {@code messages} for the group.
	 * In addition, the creating {@code timestamp} and {@code complete} flag may be used to customize
	 * the target {@link MessageGroup} object.
	 * @param messages the messages for the group.
	 * @param groupId the group id to use.
	 * @param timestamp the creation time.
	 * @param complete the {@code boolean} flag to indicate that group is completed.
	 * @return the {@link MessageGroup} instance.
	 */
	MessageGroup create(Collection<? extends Message<?>> messages, Object groupId, long timestamp,
			boolean complete);

	/**
	 * Create a {@link MessageGroup} instance based on the provided {@code groupId}.
	 * The {@link MessageGroupStore} may be consulted for the messages and metadata for the {@link MessageGroup}.
	 * @param messageGroupStore the {@link MessageGroupStore} for additional {@link MessageGroup} information.
	 * @param groupId the group id to use.
	 * @return the {@link MessageGroup} instance.
	 */
	MessageGroup create(MessageGroupStore messageGroupStore, Object groupId);

	/**
	 * Create a group backed by a basic store, which may be consulted for messages.
	 * By default, retrieves the messages eagerly.
	 * @param messageGroupStore the store for the messages.
	 * @param groupId the group id to use.
	 * @return the message group instance.
	 * @since 7.2
	 */
	default MessageGroup create(BasicMessageGroupStore messageGroupStore, Object groupId) {
		return create(messageGroupStore.getMessagesForGroup(groupId), groupId);
	}

	/**
	 * Create a {@link MessageGroup} instance based on the provided {@code groupId}.
	 * The {@link MessageGroupStore} may be consulted for the messages and metadata for the {@link MessageGroup}.
	 * In addition, the creating {@code timestamp} and {@code complete} flag may be used to customize
	 * the target {@link MessageGroup} object.
	 * @param messageGroupStore the {@link MessageGroupStore} for additional {@link MessageGroup} information.
	 * @param groupId the group id to use.
	 * @param timestamp the creation time.
	 * @param complete the {@code boolean} flag to indicate that group is completed.
	 * @return the {@link MessageGroup} instance.
	 */
	MessageGroup create(MessageGroupStore messageGroupStore, Object groupId, long timestamp,
			boolean complete);

}
