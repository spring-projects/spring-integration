/*
 * Copyright 2002-2014 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DefaultHeaderChannelRegistry;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class ControlBusTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Autowired
	private DefaultHeaderChannelRegistry registry;

	@Autowired
	private Service service;

	@Test
	public void testDefaultEvaluationContext() {
		Message<?> message = MessageBuilder.withPayload("@service.convert('aardvark')+headers.foo").setHeader("foo", "bar").build();
		this.input.send(message);
		assertEquals("catbar", output.receive(0).getPayload());
		assertNull(output.receive(0));
	}

	@Test
	public void testvoidOperation() throws Exception {
		Message<?> message = MessageBuilder.withPayload("@service.voidOp('foo')").build();
		this.input.send(message);
		assertTrue(this.service.latch.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void testLifecycleMethods() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"ControlBusLifecycleTests-context.xml", this.getClass());
		MessageChannel inputChannel = context.getBean("inputChannel", MessageChannel.class);
		PollableChannel outputChannel = context.getBean("outputChannel", PollableChannel.class);
		assertNull(outputChannel.receive(1000));
		Message<?> message = MessageBuilder.withPayload("@adapter.start()").build();
		inputChannel.send(message);
		assertNotNull(outputChannel.receive(1000));
		context.close();
	}

	@Test
	public void testControlHeaderChannelReaper() throws InterruptedException {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.convertAndSend(input, "@integrationHeaderChannelRegistry.size()");
		Message<?> result = this.output.receive(0);
		assertNotNull(result);
		assertEquals(0, result.getPayload());
		this.registry.setReaperDelay(10);
		this.registry.channelToChannelName(new DirectChannel());
		messagingTemplate.convertAndSend(input, "@integrationHeaderChannelRegistry.size()");
		result = this.output.receive(0);
		assertNotNull(result);
		assertEquals(1, result.getPayload());
		Thread.sleep(100);
		messagingTemplate.convertAndSend(input, "@integrationHeaderChannelRegistry.runReaper()");
		messagingTemplate.convertAndSend(input, "@integrationHeaderChannelRegistry.size()");
		result = this.output.receive(0);
		assertNotNull(result);
		assertEquals(0, result.getPayload());
		this.registry.setReaperDelay(60000);
	}

	@Test
	public void testRouterMappings() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(input, "@'router.handler'.getChannelMappings()");
		Message<?> result = this.output.receive(0);
		assertNotNull(result);
		Map<?, ?> mappings = (Map<?, ?>) result.getPayload();
		assertEquals("bar", mappings.get("foo"));
		assertEquals("qux", mappings.get("baz"));
		messagingTemplate.convertAndSend(input,
				"@'router.handler'.replaceChannelMappings('foo=qux \n baz=bar')");
		messagingTemplate.convertAndSend(input, "@'router.handler'.getChannelMappings()");
		result = this.output.receive(0);
		assertNotNull(result);
		mappings = (Map<?, ?>) result.getPayload();
		assertEquals("bar", mappings.get("baz"));
		assertEquals("qux", mappings.get("foo"));
	}

	public static class Service {

		private final CountDownLatch latch = new CountDownLatch(1);

		@ManagedOperation
		public String convert(String input) {
			return "cat";
		}

		@ManagedOperation
		public void voidOp(String input) {
			latch.countDown();
		}

	}

	public static class AdapterService {
		public Message<String> receive() {
			return new GenericMessage<String>(new Date().toString());
		}

	}

}
