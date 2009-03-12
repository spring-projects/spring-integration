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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class BridgeHandlerTests {

	@Test
	public void simpleBridge() {
		QueueChannel outputChannel = new QueueChannel();
		BridgeHandler handler = new BridgeHandler();
		handler.setOutputChannel(outputChannel);
		Message<?> request = new StringMessage("test");
		handler.handleMessage(request);
		Message<?> reply = outputChannel.receive(0);
		assertNotNull(reply);
		assertEquals(request, reply);
	}

	@Test(expected = IllegalStateException.class)
	public void missingOutputChannelVerifiedUponInitialization() {
		BridgeHandler handler = new BridgeHandler();
		handler.afterPropertiesSet();
	}

	@Test(expected = MessageHandlingException.class)
	public void missingOutputChannelVerifiedAtRuntime() {
		BridgeHandler handler = new BridgeHandler();
		Message<?> request = new StringMessage("test");
		handler.handleMessage(request);
	}

}
