/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.scripting.dsl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.QueueChannelOperations;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileCopyUtils;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class ScriptsTests {

	@ClassRule
	public static final TemporaryFolder FOLDER = new TemporaryFolder();

	private static File SCRIPT_FILE;

	@Autowired
	@Qualifier("scriptSplitter.input")
	private MessageChannel splitterInput;

	@Autowired
	@Qualifier("scriptTransformer.input")
	private MessageChannel transformerInput;

	@Autowired
	@Qualifier("scriptFilter.input")
	private MessageChannel filterInput;

	@Autowired
	private PollableChannel discardChannel;

	@Autowired
	@Qualifier("scriptService.input")
	private MessageChannel scriptServiceInput;

	@Autowired
	@Qualifier("scriptRouter.input")
	private MessageChannel scriptRouterInput;

	@Autowired
	private PollableChannel longStrings;

	@Autowired
	private PollableChannel shortStrings;

	@Autowired
	private PollableChannel results;

	@Autowired
	private PollableChannel messageSourceChannel;

	@BeforeClass
	public static void setup() throws IOException {
		SCRIPT_FILE = FOLDER.newFile("script.jython");
		FileCopyUtils.copy("1".getBytes(), SCRIPT_FILE);
	}

	@After
	public void clear() {
		((QueueChannelOperations) this.results).clear();
	}

	@Test
	public void splitterTest() {
		this.splitterInput.send(new GenericMessage<>("x,y,z"));
		assertEquals("x", this.results.receive(10000).getPayload());
		assertEquals("y", this.results.receive(10000).getPayload());
		assertEquals("z", this.results.receive(10000).getPayload());
	}

	@Test
	public void transformerTest() {
		for (int i = 1; i <= 3; i++) {
			this.transformerInput.send(new GenericMessage<>("test-" + i));
		}
		assertEquals("ruby-test-1-bar", this.results.receive(10000).getPayload());
		assertEquals("ruby-test-2-bar", this.results.receive(10000).getPayload());
		assertEquals("ruby-test-3-bar", this.results.receive(10000).getPayload());
	}

	@Test
	public void filterTest() {
		Message<?> message1 = MessageBuilder.withPayload("bad").setHeader("type", "bad").build();
		Message<?> message2 = MessageBuilder.withPayload("good").setHeader("type", "good").build();
		this.filterInput.send(message1);
		this.filterInput.send(message2);
		assertEquals("good", this.results.receive(10000).getPayload());
		assertNull(this.results.receive(0));
		assertEquals("bad", this.discardChannel.receive(10000).getPayload());
		assertNull(this.discardChannel.receive(0));
	}

	@Test
	public void serviceWithRefreshCheckDelayTest() throws IOException {
		this.scriptServiceInput.send(new GenericMessage<Object>("test"));
		assertEquals(1, this.results.receive(10000).getPayload());

		FileCopyUtils.copy("2".getBytes(), SCRIPT_FILE);
		SCRIPT_FILE.setLastModified(System.currentTimeMillis() + 10000); // force refresh

		this.scriptServiceInput.send(new GenericMessage<Object>("test"));
		assertEquals(2, this.results.receive(10000).getPayload());
	}

	@Test
	public void routerTest() throws IOException {
		this.scriptRouterInput.send(new GenericMessage<>("aardvark"));
		this.scriptRouterInput.send(new GenericMessage<>("bear"));
		this.scriptRouterInput.send(new GenericMessage<>("cat"));
		this.scriptRouterInput.send(new GenericMessage<>("dog"));
		this.scriptRouterInput.send(new GenericMessage<>("elephant"));
		assertEquals("bear", this.shortStrings.receive(10000).getPayload());
		assertEquals("cat", this.shortStrings.receive(10000).getPayload());
		assertEquals("dog", this.shortStrings.receive(10000).getPayload());
		assertEquals("aardvark", this.longStrings.receive(10000).getPayload());
		assertEquals("elephant", this.longStrings.receive(10000).getPayload());
	}

	@Test
	public void messageSourceTest() throws IOException, InterruptedException {
		Message<?> message = this.messageSourceChannel.receive(20000);
		assertNotNull(message);
		Object payload = message.getPayload();
		assertThat(payload, Matchers.instanceOf(Date.class));

		// Some time window to avoid dates collision
		Thread.sleep(500);

		assertTrue(((Date) payload).before(new Date()));

		assertNotNull(this.messageSourceChannel.receive(20000));

		assertNull(this.messageSourceChannel.receive(10));
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Value("scripts/TesSplitterScript.groovy")
		private Resource splitterScript;

		@Value("scripts/TestFilterScript.groovy")
		private Resource filterScript;

		@Bean
		public PollableChannel results() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow scriptSplitter() {
			return f -> f.split(Scripts.processor(this.splitterScript)).channel(results());
		}

		@Bean
		public IntegrationFlow scriptTransformer() {
			return f -> f
					.transform(Scripts.processor("scripts/TestTransformerScript.rb")
							.lang("ruby")
							.variable("foo", "bar"))
					.channel(results());
		}

		@Bean
		public PollableChannel discardChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow scriptFilter() {
			return f -> f.filter(Scripts.processor(this.filterScript), e -> e.discardChannel("discardChannel"))
					.channel(results());
		}

		@Bean
		public IntegrationFlow scriptService() {
			return f -> f.handle(Scripts.processor("file:" + SCRIPT_FILE.getAbsolutePath()).refreshCheckDelay(0))
					.channel(results());
		}

		@Bean
		public IntegrationFlow scriptRouter() {
			return f -> f.route(Scripts.processor("scripts/TestRouterScript.js"));
		}

		@Bean
		public PollableChannel longStrings() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel shortStrings() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow scriptPollingAdapter() {
			return IntegrationFlows
					.from(Scripts.messageSource("scripts/TestMessageSourceScript.ruby"),
							e -> e.poller(p -> p.fixedDelay(100)))
					.channel(c -> c.queue("messageSourceChannel"))
					.get();
		}

	}

}
