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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PollerAnnotationConsumerAdviceChainTests {

	@Autowired @Qualifier("input")
	private MessageChannel input;

	@Autowired @Qualifier("output")
	private PollableChannel output;

	@Autowired
	private CountDownLatch latch;

	@Autowired
	private TestBeforeAdvice beforeAdvice;

	@Autowired
	private TestAfterAdvice afterAdvice;

	@Autowired
	private TestAroundAdvice aroundAdvice;


	@Test
	public void testAdviceChain() throws InterruptedException {
		input.send(new StringMessage("test"));
		Message<?> reply = output.receive(3000);
		assertEquals("TEST", reply.getPayload());
		latch.await(1, TimeUnit.SECONDS);
		assertEquals(0, latch.getCount());
		assertEquals(4, beforeAdvice.getLatchCount());
		assertEquals(3, aroundAdvice.getPreCount());
		assertEquals(2, afterAdvice.getLatchCount());
		assertEquals(1, aroundAdvice.getPostCount());
	}

}
