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

package org.springframework.integration.router.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class HeaderValueRouterConvertibleTypeTests {

	@Autowired
	private MessageChannel inputChannel;

	@Autowired
	private PollableChannel trueChannel;

	@Autowired
	private PollableChannel falseChannel;


	@Test
	public void testBooleanValueMappingToChannels() {
		Message<?> trueMessage = MessageBuilder.withPayload(1)
				.setHeader("testHeader", true).build();
		Message<?> falseMessage = MessageBuilder.withPayload(0)
				.setHeader("testHeader", false).build();
		inputChannel.send(trueMessage);
		inputChannel.send(falseMessage);
		Message<?> trueResult = trueChannel.receive();
		assertNotNull(trueResult);
		assertEquals(1, trueResult.getPayload());
		Message<?> falseResult = falseChannel.receive();
		assertNotNull(falseResult);
		assertEquals(0, falseResult.getPayload());
	}

}
