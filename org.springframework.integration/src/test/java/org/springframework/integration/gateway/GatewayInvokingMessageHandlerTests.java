/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.gateway;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.StringMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GatewayInvokingMessageHandlerTests {

	@Autowired
	@Qualifier("inputA")
	SubscribableChannel channel;
	
	@Autowired
	@Qualifier("simpleGateway")
	SimpleGateway gateway;
	
	@Autowired
	@Qualifier("inputB")
	SubscribableChannel output;

	@Test
	public void validateGatewayInTheChainViaChannel() {
		output.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				Assert.assertEquals("echo:echo:hello", message.getPayload());
				Assert.assertEquals("foo", message.getHeaders().get("foo"));
				Assert.assertEquals("oleg", message.getHeaders().get("name"));
			}
		});
		channel.send(new StringMessage("hello"));
	}

	@Test
	public void validateGatewayInTheChainViaAnotherGateway() {	
		output.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				Assert.assertEquals("echo:echo:hello", message.getPayload());
				Assert.assertEquals("foo", message.getHeaders().get("foo"));
				Assert.assertEquals("oleg", message.getHeaders().get("name"));
			}
		});
		String result = gateway.sendRecieve("hello");
		Assert.assertEquals("echo:echo:hello", result);
	}

	public static interface SimpleGateway {
		public String sendRecieve(String str);
	}

	public static class SimpleService {
		public String echo(String str) {
			return "echo:" + str;
		}
	}

}
