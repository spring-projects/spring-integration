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

package org.springframework.integration.bus;

import org.springframework.context.ApplicationEvent;

/**
 * Event raised when a <code>MessageBus</code> is stopped.
 * 
 * @author Mark Fisher
 */
public class MessageBusStoppedEvent extends ApplicationEvent {

	/**
	 * Create a new MessageBusStoppedEvent
	 * @param source the <code>MessageBus</code> that has been stopped
	 * (must not be <code>null</code>)
	 */
	public MessageBusStoppedEvent(MessageBus source) {
		super(source);
	}

}
