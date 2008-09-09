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

package org.springframework.integration.samples.cafe;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.MessageBuilder;

/**
 * The entry point for Cafe Demo. When the '<code>placeOrder</code>'
 * method is invoked, it passes the {@link Order} as the payload of a
 * {@link org.springframework.integration.message.Message} to the
 * 'orderChannel'. The channel reference is configured in the "cafe" bean
 * definition within 'cafeDemo.xml'.
 * 
 * @author Mark Fisher
 */
public class Cafe {

	private MessageChannel orderChannel;


	public void setOrderChannel(MessageChannel orderChannel) {
		this.orderChannel = orderChannel;
	}

	public void placeOrder(Order order) {
		this.orderChannel.send(MessageBuilder.fromPayload(order).build());
	}

}
