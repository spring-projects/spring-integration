/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import java.util.Collection;

/**
 * MessageHandlers implementing this interface can propagate headers from
 * an input message to an output message.
 *
 * @author Gary Russell
 * @since 4.3.11
 *
 */
public interface HeaderPropagationAware {

	/**
	 * Set headers that will NOT be copied from the inbound message if
	 * the handler is configured to copy headers.
	 * @param headers the headers to not propagate from the inbound message.
	 */
	void setNotPropagatedHeaders(String... headers);

	/**
	 * Get the header names this handler doesn't propagate.
	 * @return an immutable {@link java.util.Collection} of headers that will not be
	 * copied from the inbound message if the handler is configured to copy headers.
	 * @see #setNotPropagatedHeaders(String...)
	 */
	Collection<String> getNotPropagatedHeaders();

	/**
	 * Add headers that will NOT be copied from the inbound message if
	 * the handler is configured to copy headers, instead of overwriting
	 * the existing set.
	 * @param headers the headers to not propagate from the inbound message.
	 * @see #setNotPropagatedHeaders(String...)
	 */
	void addNotPropagatedHeaders(String... headers);

}
