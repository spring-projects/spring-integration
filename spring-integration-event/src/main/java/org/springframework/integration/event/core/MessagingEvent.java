/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.event.core;

import org.springframework.context.ApplicationEvent;
import org.springframework.messaging.Message;

/**
 * A subclass of {@link ApplicationEvent} that wraps a {@link Message}.
 * 
 * @author Mark Fisher
 */
public class MessagingEvent extends ApplicationEvent {

	private static final long serialVersionUID = -872581247155846293L;

	public MessagingEvent(Message<?> message) {
		super(message);
	}

	public Message<?> getMessage() {
		return (Message<?>) this.getSource();
	}

}
