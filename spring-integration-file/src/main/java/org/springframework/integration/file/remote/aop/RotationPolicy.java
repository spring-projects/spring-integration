/*
 * Copyright 2019-present the original author or authors.
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

package org.springframework.integration.file.remote.aop;

import org.springframework.integration.core.MessageSource;

/**
 * A strategy for rotating advices to allow reconfiguring
 * the message source before and/or after a poll.
 *
 * @author Gary Russell
 * @author Michael Forstner
 * @author Artem Bilan
 * @author David Turanski
 *
 * @since 5.2
 */
public interface RotationPolicy {

	/**
	 * Invoked before the message source receive() method.
	 * @param source the message source.
	 */
	void beforeReceive(MessageSource<?> source);

	/**
	 * Invoked after the message source receive() method.
	 * @param messageReceived true if a message was received.
	 * @param source the message source.
	 */
	void afterReceive(boolean messageReceived, MessageSource<?> source);

	/**
	 * Return the current {@link KeyDirectory}.
	 * @return the current {@link KeyDirectory}
	 * @since 5.2
	 */
	KeyDirectory getCurrent();

	/**
	 * A key for a thread-local store and its related directory pair.
	 * @param key the for entry
	 * @param directory the directory for entry
	 */
	record KeyDirectory(Object key, String directory) {

	}

}
