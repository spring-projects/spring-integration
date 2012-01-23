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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 1.0.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RecipientListRouterParserTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	@Qualifier("routingChannelA")
	private MessageChannel channel;

	@Autowired
	private MessageChannel simpleDynamicInput;

	@Autowired
	private MessageChannel noSelectorMatchInput;

	@Test
	public void checkMessageRouting() {
		context.start();
		Message<?> message = new GenericMessage<Integer>(1);
		channel.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		assertTrue(chanel1.receive(0).getPayload().equals(1));
		assertTrue(chanel2.receive(0).getPayload().equals(1));
	}

	@Test
	public void simpleRouter() {
		Object endpoint = context.getBean("simpleRouter");
		Object handler = TestUtils.getPropertyValue(endpoint, "handler");
		assertEquals(RecipientListRouter.class, handler.getClass());
		RecipientListRouter router = (RecipientListRouter) handler;
		DirectFieldAccessor accessor = new DirectFieldAccessor(router);
		assertEquals(new Long(-1), new DirectFieldAccessor(
				accessor.getPropertyValue("messagingTemplate")).getPropertyValue("sendTimeout"));
		assertEquals(Boolean.FALSE, accessor.getPropertyValue("applySequence"));
		assertEquals(Boolean.FALSE, accessor.getPropertyValue("ignoreSendFailures"));
	}

	@Test
	public void customRouter() {
		Object endpoint = context.getBean("customRouter");
		Object handler = TestUtils.getPropertyValue(endpoint, "handler");
		assertEquals(RecipientListRouter.class, handler.getClass());
		RecipientListRouter router = (RecipientListRouter) handler;
		DirectFieldAccessor accessor = new DirectFieldAccessor(router);
		assertEquals(new Long(1234), new DirectFieldAccessor(
				accessor.getPropertyValue("messagingTemplate")).getPropertyValue("sendTimeout"));
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("applySequence"));
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("ignoreSendFailures"));
	}

	@Test
	public void simpleDynamicRouter() {
		context.start();
		Message<?> message = new GenericMessage<Integer>(1);
		simpleDynamicInput.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		assertTrue(chanel1.receive(0).getPayload().equals(1));
		assertNull(chanel2.receive(0));
	}

	@Test
	public void noSelectorMatchRouter() {
		context.start();
		Message<?> message = new GenericMessage<Integer>(1);
		noSelectorMatchInput.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		Message<?> output = chanel1.receive(0);
		assertNotNull(output);
		assertTrue(output.getPayload().equals(1));
		assertNull(chanel2.receive(0));
	}

	public static class TestBean {

		public boolean accept(int number) {
			return number == 1;
		}
	}

}
