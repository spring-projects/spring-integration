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

import java.util.List;
import java.util.UUID;

import org.springframework.integration.core.Message;

/**
 * Strategy interface for storing and retrieving messages. The interface mimics
 * the semantics for REST for the methods named after REST operations. This is
 * helpful when mapping to a RESTful API.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Dave Syer
 * @since 2.0
 */
public interface MessageStore {

	/**
	 * Return the Message with the given id, or <i>null</i> if no
	 * Message with that id exists in the MessageStore.
	 */
	Message<?> get(UUID id);

	/**
	 * Put the provided Message into the MessageStore. Its id will
	 * be used as an index so that the {@link #get(UUID)} and
	 * {@link #delete(Object)} behave properly. If available, its
	 * correlationId header will also be stored so that the
	 * {@link #list(Object)} method behaves properly.
	 */
	<T> Message<T> put(Message<T> message);

	/**
	 * Remove the Message with the given id from the MessageStore,
	 * if present, and return it. If no Message with that id is
	 * present in the store, this will return <i>null</i>.
	 */
	Message<?> delete(UUID id);

	/**
	 * Return all Messages currently in the MessageStore that
	 * contain the provided correlationId header value.
	 * @see org.springframework.integration.core.MessageHeaders#getCorrelationId()
	 */
	List<Message<?>> list(Object correlationId);

}
