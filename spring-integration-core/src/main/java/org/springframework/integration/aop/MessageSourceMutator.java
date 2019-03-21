/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.integration.aop;

import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.Message;

/**
 * An object that can mutate a {@link MessageSource} before and/or after
 * {@link MessageSource#receive()} is called.
 *
 * @author Gary Russell
 *
 * @since 5.0.7.
 *
 */
@FunctionalInterface
public interface MessageSourceMutator {

	/**
	 * Subclasses can decide whether to proceed with this poll.
	 * @param source the message source.
	 * @return true to proceed (default).
	 */
	default boolean beforeReceive(MessageSource<?> source) {
		return true;
	}

	/**
	 * Subclasses can take actions based on the result of the poll; e.g.
	 * adjust the {@code trigger}. The message can also be replaced with a new one.
	 * @param result the received message.
	 * @param source the message source.
	 * @return a message to continue to process the result, null to discard whatever the poll returned.
	 */
	Message<?> afterReceive(Message<?> result, MessageSource<?> source);

}
