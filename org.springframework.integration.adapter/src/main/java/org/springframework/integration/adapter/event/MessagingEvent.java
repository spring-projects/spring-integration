/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.context.ApplicationEvent;
import org.springframework.integration.message.Message;

/**
 * A subclass of {@link ApplicationEvent} that wraps a {@link Message}.
 * 
 * @author Mark Fisher
 */
public class MessagingEvent<T> extends ApplicationEvent {

	public MessagingEvent(Message<T> message) {
		super(message);
	}

	public Message<T> getMessage() {
		return (Message<T>) this.getSource();
	}

}
