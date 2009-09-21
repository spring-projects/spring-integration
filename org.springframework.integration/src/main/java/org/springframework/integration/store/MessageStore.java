/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.integration.core.Message;

/**
 * Strategy interface for storing and retrieving messages. The interface mimics
 * the semantics for REST for the methods named after REST operations. This is
 * helpful when mapping to a RESTful api.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public interface MessageStore {

	Message<?> get(Object key);

	<T> Message<T> put(Message<T> message);

	<T> Message<T> post(T payload);

	Message<?> delete(Object key);

	List<Message<?>> list();

	List<Message<?>> getAll(Object correlationKey);

}
