/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.message;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * A message implementation that is produced by an advice after
 * successful message handling.
 * Contains the result of the expression evaluation in the payload
 * and the original message that the advice passed to the
 * handler.
 * .
 * @author Gary Russell
 * @since 2.2
 */
public class AdviceMessage extends GenericMessage<Object> {

	private static final long serialVersionUID = 1L;

	private final Message<?> inputMessage;

	public AdviceMessage(Object payload, Message<?> inputMessage) {
		super(payload);
		this.inputMessage = inputMessage;
	}

	public AdviceMessage(Object payload, Map<String, Object> headers, Message<?> inputMessage) {
		super(payload, headers);
		this.inputMessage = inputMessage;
	}

	public Message<?> getInputMessage() {
		return inputMessage;
	}

}
