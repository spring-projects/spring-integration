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

package org.springframework.integration.adapter.event;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageMapper;

/**
 * Maps between {@link Message Messages} and {@link MessagingEvent MessagingEvents}.
 * 
 * @author Mark Fisher
 */
public class MessagingEventMapper<T> implements MessageCreator<MessagingEvent<T>, T>, MessageMapper<T, MessagingEvent<T>> {

	public Message<T> createMessage(MessagingEvent<T> event) {
		return event.getMessage();
	}

	public MessagingEvent<T> mapMessage(Message<T> message) {
		return new MessagingEvent<T>(message);
	}

}
