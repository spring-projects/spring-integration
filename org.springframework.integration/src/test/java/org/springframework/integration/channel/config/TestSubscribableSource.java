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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.SubscribableSource;

/**
 * @author Mark Fisher
 */
public class TestSubscribableSource implements SubscribableSource {

	private final List<MessageEndpoint> endpoints = new CopyOnWriteArrayList<MessageEndpoint>();


	public boolean subscribe(MessageEndpoint endpoint) {
		return this.endpoints.add(endpoint);
	}

	public boolean unsubscribe(MessageEndpoint endpoint) {
		return this.endpoints.remove(endpoint);
	}

	public void publishMessage(Message<?> message) {
		for (MessageEndpoint endpoint : this.endpoints) {
			endpoint.send(message);
		}
	}

}
