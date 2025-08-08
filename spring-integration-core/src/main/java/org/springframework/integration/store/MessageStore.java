/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.store;

import java.util.UUID;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;

/**
 * Strategy interface for storing and retrieving messages.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 */
public interface MessageStore {

	/**
	 * @param id The message identifier.
	 * @return The Message with the given id, or <i>null</i> if no Message with that id exists in the MessageStore.
	 */
	Message<?> getMessage(UUID id);

	/**
	 * Return a {@link MessageMetadata} for the {@link Message} by provided {@code id}.
	 * @param id The message identifier.
	 * @return The MessageMetadata with the given id, or <i>null</i>
	 * if no Message with that id exists in the MessageStore
	 * or the message has no metadata (legacy message from an earlier version).
	 * @since 5.0
	 */
	MessageMetadata getMessageMetadata(UUID id);

	/**
	 * Put the provided Message into the MessageStore. The store may need to mutate the message internally, and if it
	 * does then the return value can be different than the input. The id of the return value will be used as an index
	 * so that the {@link #getMessage(UUID)} and {@link #removeMessage(UUID)} behave properly. Since messages are
	 * immutable, putting the same message more than once is a no-op.
	 *
	 * @param message The message.
	 * @param <T> The payload type.
	 * @return The message that was stored.
	 */
	<T> Message<T> addMessage(Message<T> message);

	/**
	 * Remove the Message with the given id from the MessageStore, if present, and return it.
	 * If no Message with that id is present in the store, this will return {@code null}.
	 * If this method is implemented on a {@link MessageGroupStore},
	 * the message is removed from the store only if no groups holding this message.
	 * @param id the message identifier.
	 * @return the message (if any).
	 */
	Message<?> removeMessage(UUID id);

	/**
	 * Optional attribute giving the number of messages in the store. Implementations may decline to respond by throwing
	 * an exception.
	 *
	 * @return The number of messages.
	 * @throws UnsupportedOperationException if not implemented
	 */
	@ManagedAttribute
	long getMessageCount();

}
