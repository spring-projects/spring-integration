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

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.Subscribable;

/**
 * @author Mark Fisher
 */
public class TestSubscribableSource implements Subscribable {

	private final List<MessageConsumer> subscibers = new CopyOnWriteArrayList<MessageConsumer>();


	public boolean subscribe(MessageConsumer subsciber) {
		return this.subscibers.add(subsciber);
	}

	public boolean unsubscribe(MessageConsumer subsciber) {
		return this.subscibers.remove(subsciber);
	}

	public void publishMessage(Message<?> message) {
		for (MessageConsumer subsciber : this.subscibers) {
			subsciber.onMessage(message);
		}
	}

}
