/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 1.0.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DelayerUsageTests {

	@Autowired @Qualifier("inputA")
	private MessageChannel inputA;

	@Autowired
	private MessageChannel delayerInsideChain;

	@Autowired @Qualifier("outputA")
	private PollableChannel outputA;

	@Autowired @Qualifier("inputB")
	private MessageChannel inputB;

	@Autowired @Qualifier("outputB1")
	private PollableChannel outputB1;

	@Autowired
	private MessageHandlerChain chainWithDelayer;

	@Test
	public void testDelayWithDefaultScheduler(){
		long start = System.currentTimeMillis();
		inputA.send(new GenericMessage<String>("Hello"));
		outputA.receive();
		assertTrue((System.currentTimeMillis() - start) >= 1000);
	}

	@Test
	public void testDelayWithDefaultSchedulerCustomDelayHeader(){
		MessageBuilder<String> builder = MessageBuilder.withPayload("Hello");
		// set custom delay header
		builder.setHeader("foo", 2000);
		long start = System.currentTimeMillis();
		inputA.send(builder.build());
		outputA.receive();
		assertTrue((System.currentTimeMillis() - start) >= 2000);
	}

	@Test
	public void testDelayWithCustomScheduler(){
		long start = System.currentTimeMillis();
		inputB.send(new GenericMessage<String>("1"));
		inputB.send(new GenericMessage<String>("2"));
		inputB.send(new GenericMessage<String>("3"));
		inputB.send(new GenericMessage<String>("4"));
		inputB.send(new GenericMessage<String>("5"));
		inputB.send(new GenericMessage<String>("6"));
		inputB.send(new GenericMessage<String>("7"));
		outputB1.receive();
		outputB1.receive();
		outputB1.receive();
		outputB1.receive();
		outputB1.receive();
		outputB1.receive();
		outputB1.receive();

		// must execute under 3 seconds, since threadPool is set too 5.
		// first batch is 5 concurrent invocations on SA, then 2 more
		// elapsed time for the whole execution should be a bit over 2 seconds depending on the hardware
		assertTrue(((System.currentTimeMillis() - start) >= 1000) && ((System.currentTimeMillis() - start) < 3000));
	}

	@Test //INT-1132
	public void testDelayerInsideChain(){
		//TODO 'SmartLifecycle' isn't invoked by 'MessageHandlerChain'
		@SuppressWarnings("unchecked")
		List<MessageHandler> handlers = TestUtils.getPropertyValue(chainWithDelayer, "handlers", List.class);
		SmartLifecycle delayHandler = (SmartLifecycle) handlers.get(1);
		delayHandler.start();

		long start = System.currentTimeMillis();
		delayerInsideChain.send(new GenericMessage<String>("Hello"));
		Message<?> message = outputA.receive();
		assertTrue((System.currentTimeMillis() - start) >= 1000);
		assertEquals("hello", message.getPayload());
	}

	public static class SampleService{
		public String processMessage(String message) throws Exception {
			Thread.sleep(500);
			return message;
		}
	}
}
