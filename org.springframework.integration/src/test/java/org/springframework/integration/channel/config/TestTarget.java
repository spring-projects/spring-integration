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

package org.springframework.integration.channel.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;

/**
 * @author Mark Fisher
 */
public class TestTarget implements MessageTarget {

	private final List<Message<?>> receivedMessages = new ArrayList<Message<?>>();


	public List<Message<?>> getReceivedMessages() {
		return this.receivedMessages;
	}

	public boolean send(Message<?> message) {
		this.receivedMessages.add(message);
		return true;
	}

}
