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
package org.springframework.integration.osgi;

import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.controlbus.ControlBus;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;

/**
 * Implementation of the {@link ControlBus} interface. 
 * Control Bus itself wrapper over {@link SubscribableChannel}, 
 * which represents the entry point to Control Bus infrastructure.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class OSGiIntegrationControlBus implements ControlBus {
	private SubscribableChannel channel;
	private String busName;
	
	public OSGiIntegrationControlBus(SubscribableChannel channel, String busName){
		this.channel = channel;
		this.busName = busName;
	}

	public boolean subscribe(MessageHandler handler) {
		return channel.subscribe(handler);
	}

	
	public boolean unsubscribe(MessageHandler handler) {
		return channel.unsubscribe(handler);
	}

	public String getName() {
		return channel.getName();
	}

	
	public boolean send(Message<?> message) {
		return channel.send(message);
	}

	
	public boolean send(Message<?> message, long timeout) {
		return channel.send(message, timeout);
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.controlbus.ControlBus#isBusAvailable()
	 */
	public boolean isBusAvailable() {
		return true;
	}
}
