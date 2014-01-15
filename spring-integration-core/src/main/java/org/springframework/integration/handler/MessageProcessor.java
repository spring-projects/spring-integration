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

package org.springframework.integration.handler;

import org.springframework.messaging.Message;

/**
 * This defines the lowest-level strategy of processing a Message and returning
 * some Object (or null). Implementations will be focused on generic concerns,
 * such as invoking a method, running a script, or evaluating an expression.
 * <p>
 * Higher level MessageHandler implementations can delegate to these processors
 * for such functionality, but it is the responsibility of each handler type to
 * add the semantics such as routing, splitting, transforming, etc.
 * <p>
 * In some cases the return value might be a Message itself, but it does not
 * need to be. It is the responsibility of the caller to determine how to treat
 * the return value. That may require creating a Message or even creating
 * multiple Messages from that value.
 * <p>
 * This strategy and its various implementations are considered part of the
 * internal "support" API, intended for use by Spring Integration's various
 * message-handling components. As such, it is subject to change.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public interface MessageProcessor<T> {

	/**
	 * Process the Message and return a value (or null).
	 *
	 * @param message The message to process.
	 * @return The result.
	 */
	T processMessage(Message<?> message);

}
