/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.xml;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DefaultHeaderChannelRegistry;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
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
		assertThat(output.receive(0).getPayload()).isEqualTo("catbar");
		assertThat(output.receive(0)).isNull();
	}

	@Test
	public void testvoidOperation() throws Exception {
		Message<?> message = MessageBuilder.withPayload("@service.voidOp('foo')").build();
		this.input.send(message);
		assertThat(this.service.latch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testLifecycleMethods() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"ControlBusLifecycleTests-context.xml", this.getClass());
		MessageChannel inputChannel = context.getBean("inputChannel", MessageChannel.class);
		PollableChannel outputChannel = context.getBean("outputChannel", PollableChannel.class);
		assertThat(outputChannel.receive(10)).isNull();
		Message<?> message = MessageBuilder.withPayload("@adapter.start()").build();
		inputChannel.send(message);
		assertThat(outputChannel.receive(1000)).isNotNull();
		context.close();
	}

	@Test
	public void testControlHeaderChannelReaper() throws InterruptedException {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.convertAndSend(input, "@integrationHeaderChannelRegistry.size()");
		Message<?> result = this.output.receive(0);
		assertThat(result).isNotNull();
		// No channels in the registry
		assertThat(result.getPayload()).isEqualTo(0);
		this.registry.channelToChannelName(new DirectChannel());
		// Sleep a bit to be sure that we aren't reaped by registry TTL as 60000
		Thread.sleep(10);
		messagingTemplate.convertAndSend(input, "@integrationHeaderChannelRegistry.size()");
		result = this.output.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(1);
		// Some DirectFieldAccessor magic to modify 'expireAt' to the past to avoid timing issues on high-loaded build
		Object messageChannelWrapper =
				TestUtils.getPropertyValue(this.registry, "channels", Map.class).values().iterator().next();
		DirectFieldAccessor dfa = new DirectFieldAccessor(messageChannelWrapper);
		dfa.setPropertyValue("expireAt", System.currentTimeMillis() - 60000);
		messagingTemplate.convertAndSend(input, "@integrationHeaderChannelRegistry.runReaper()");
		messagingTemplate.convertAndSend(input, "@integrationHeaderChannelRegistry.size()");
		result = this.output.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(0);
	}

	@Test
	public void testRouterMappings() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(input, "@'router.handler'.getChannelMappings()");
		Message<?> result = this.output.receive(0);
		assertThat(result).isNotNull();
		Map<?, ?> mappings = (Map<?, ?>) result.getPayload();
		assertThat(mappings.get("foo")).isEqualTo("bar");
		assertThat(mappings.get("baz")).isEqualTo("qux");
		messagingTemplate.convertAndSend(input,
				"@'router.handler'.replaceChannelMappings('foo=qux \n baz=bar')");
		messagingTemplate.convertAndSend(input, "@'router.handler'.getChannelMappings()");
		result = this.output.receive(0);
		assertThat(result).isNotNull();
		mappings = (Map<?, ?>) result.getPayload();
		assertThat(mappings.get("baz")).isEqualTo("bar");
		assertThat(mappings.get("foo")).isEqualTo("qux");
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
