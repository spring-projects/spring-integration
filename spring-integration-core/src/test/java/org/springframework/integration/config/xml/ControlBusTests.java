/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DefaultHeaderChannelRegistry;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @author Gunnar Hillert
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ControlBusTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Autowired
	private DefaultHeaderChannelRegistry registry;

	@Test
	public void testDefaultEvaluationContext() {
		Message<?> message = MessageBuilder.withPayload("@service.convert('aardvark')+headers.foo").setHeader("foo", "bar").build();
		this.input.send(message);
		assertEquals("catbar", output.receive(0).getPayload());
		assertNull(output.receive(0));
	}

	@Test
	public void testLifecycleMethods() {
		ApplicationContext context = new ClassPathXmlApplicationContext("ControlBusLifecycleTests-context.xml", this.getClass());
		MessageChannel inputChannel = context.getBean("inputChannel", MessageChannel.class);
		PollableChannel outputChannel = context.getBean("outputChannel", PollableChannel.class);
		assertNull(outputChannel.receive(1000));
		Message<?> message = MessageBuilder.withPayload("@adapter.start()").build();
		inputChannel.send(message);
		assertNotNull(outputChannel.receive(1000));
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

	public static class Service {

		@ManagedOperation
		public String convert(String input) {
			return "cat";
		}

	}

	public static class AdapterService {
		public Message<String> receive() {
			return new GenericMessage<String>(new Date().toString());
		}

	}

}
