/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.store;

import java.util.Collection;
import java.util.UUID;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;

/**
 * Strategy interface for storing and retrieving messages. The interface mimics the semantics for REST for the methods
 * named after REST operations. This is helpful when mapping to a RESTful API.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Dave Syer
 * @since 2.0
 */
public interface MessageStore {

	String PROCESSED = MessageHeaders.PREFIX + "processed";

	/**
	 * Return the Message with the given id, or <i>null</i> if no Message with that id exists in the MessageStore.
	 */
	Message<?> get(UUID id);

	/**
	 * Put the provided Message into the MessageStore. The store may need to mutate the message internally, and if it
	 * does then the return value can be different than the input. The id of the return value will be used as an index
	 * so that the {@link #get(UUID)} and {@link #delete(Object)} behave properly. Since messages are immutable, putting
	 * the same message more than once is a no-op.
	 * 
	 * @return the message that was stored
	 */
	<T> Message<T> put(Message<T> message);

	/**
	 * Remove the Message with the given id from the MessageStore, if present, and return it. If no Message with that id
	 * is present in the store, this will return <i>null</i>.
	 */
	Message<?> delete(UUID id);

	/**
	 * Return all Messages currently in the MessageStore that were stored using {@link #put(Object, Message)} or
	 * {@link #put(Object, Collection)} with this correlation id.
	 * 
	 * @see org.springframework.integration.core.MessageHeaders#getCorrelationId()
	 */
	Collection<Message<?>> list(Object correlationId);

	/**
	 * Store a message with an association to a correlation id. This can be used to group messages together instead of
	 * storing them just under their id.
	 * 
	 * @param correlationId the correlation id to store the message under
	 * @param message a message
	 */
	void put(Object correlationId, Message<?> message);

	/**
	 * Mark a message from the association with this correlation id. If the message was previously added using
	 * {@link #put(Object, Message)} then it will be removed and re-inserted with the {@value #PROCESSED} flag set in
	 * the headers.
	 * 
	 * @param correlationId the correlation id to mark the message under
	 */
	Message<?> mark(Object correlationId, UUID messageId);

	/**
	 * Delete all the messages from the association with this correlation id. If the messages were stored under their id
	 * through {@link #put(Message)} they are still accessible via {@link #get(UUID)}.
	 * 
	 * @param correlationId the correlation id to delete all messages under
	 */
	void deleteAll(Object correlationId);

}
