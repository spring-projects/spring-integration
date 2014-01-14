/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.UUID;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;

/**
 * Strategy interface for storing and retrieving messages.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Dave Syer
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
	 * Remove the Message with the given id from the MessageStore, if present, and return it. If no Message with that id
	 * is present in the store, this will return <i>null</i>.
	 *
	 * @param id THe message identifier.
	 * @return The message.
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
