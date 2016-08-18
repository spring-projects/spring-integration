/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.core;

import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
public interface LimitingMessageSource<T> extends MessageSource<T> {

	/**
	 * Retrieve the next available message from this source. For sources retrieving data
	 * from a remote system the callback is invoked to determine the maximum number of
	 * objects to fetch. Returns <code>null</code> if no message is available.
	 * @param maxFetchSize the maximum number of objects to fetch.
	 * @return The message or null.
	 */
	Message<T> receive(int maxFetchSize);

}
