/*
 * Copyright 2002-2024 the original author or authors.
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

import org.springframework.messaging.Message;

/**
 * Implementations of this interface are subclasses of
 * {@link AbstractMessageHandler} that perform post processing after the
 * {@link AbstractMessageHandler#handleMessageInternal(org.springframework.messaging.Message)}
 * call.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public interface PostProcessingMessageHandler {

	/**
	 * Take some further action on the result and/or message.
	 * @param result The result from {@link AbstractMessageHandler#handleMessageInternal(Message)}.
	 * @param message The message.
	 * @return The post-processed result.
	 */
	Object postProcess(Message<?> message, Object result);

}
