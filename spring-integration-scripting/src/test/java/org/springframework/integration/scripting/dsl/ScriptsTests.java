/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.scripting.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class ScriptsTests {

	@TempDir
	public static File FOLDER;

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

	@BeforeAll
	public static void setup() throws IOException {
		SCRIPT_FILE = new File(FOLDER, "script.py");
		FileCopyUtils.copy("1".getBytes(), SCRIPT_FILE);
	}

	@AfterEach
	public void clear() {
		((QueueChannelOperations) this.results).clear();
	}

	@Test
	public void splitterTest() {
		this.splitterInput.send(new GenericMessage<>("x,y,z"));
		assertThat(this.results.receive(10000).getPayload()).isEqualTo("x");
		assertThat(this.results.receive(10000).getPayload()).isEqualTo("y");
		assertThat(this.results.receive(10000).getPayload()).isEqualTo("z");
	}

	@Test
	public void transformerTest() {
		for (int i = 1; i <= 3; i++) {
			this.transformerInput.send(new GenericMessage<>("test-" + i));
		}
		assertThat(this.results.receive(10000).getPayload()).isEqualTo("ruby-test-1-bar");
		assertThat(this.results.receive(10000).getPayload()).isEqualTo("ruby-test-2-bar");
		assertThat(this.results.receive(10000).getPayload()).isEqualTo("ruby-test-3-bar");
	}

	@Test
	public void filterTest() {
		Message<?> message1 = MessageBuilder.withPayload("bad").setHeader("type", "bad").build();
		Message<?> message2 = MessageBuilder.withPayload("good").setHeader("type", "good").build();
		this.filterInput.send(message1);
		this.filterInput.send(message2);
		assertThat(this.results.receive(10000).getPayload()).isEqualTo("good");
		assertThat(this.results.receive(0)).isNull();
		assertThat(this.discardChannel.receive(10000).getPayload()).isEqualTo("bad");
		assertThat(this.discardChannel.receive(0)).isNull();
	}

	@Test
	public void serviceWithRefreshCheckDelayTest() throws IOException {
		this.scriptServiceInput.send(new GenericMessage<Object>("test"));
		assertThat(this.results.receive(10000).getPayload()).isEqualTo(1);

		FileCopyUtils.copy("2".getBytes(), SCRIPT_FILE);
		SCRIPT_FILE.setLastModified(System.currentTimeMillis() + 10000); // force refresh

		this.scriptServiceInput.send(new GenericMessage<Object>("test"));
		assertThat(this.results.receive(10000).getPayload()).isEqualTo(2);
	}

	@Test
	public void routerTest() {
		this.scriptRouterInput.send(new GenericMessage<>("aardvark"));
		this.scriptRouterInput.send(new GenericMessage<>("bear"));
		this.scriptRouterInput.send(new GenericMessage<>("cat"));
		this.scriptRouterInput.send(new GenericMessage<>("dog"));
		this.scriptRouterInput.send(new GenericMessage<>("elephant"));
		assertThat(this.shortStrings.receive(10000).getPayload()).isEqualTo("bear");
		assertThat(this.shortStrings.receive(10000).getPayload()).isEqualTo("cat");
		assertThat(this.shortStrings.receive(10000).getPayload()).isEqualTo("dog");
		assertThat(this.longStrings.receive(10000).getPayload()).isEqualTo("aardvark");
		assertThat(this.longStrings.receive(10000).getPayload()).isEqualTo("elephant");
	}

	@Test
	public void messageSourceTest() throws InterruptedException {
		Message<?> message = this.messageSourceChannel.receive(20000);
		assertThat(message).isNotNull();
		Object payload = message.getPayload();
		assertThat(payload).isInstanceOf(Date.class);

		// Some time window to avoid dates collision
		Thread.sleep(2);

		assertThat(((Date) payload).before(new Date())).isTrue();

		assertThat(this.messageSourceChannel.receive(20000)).isNotNull();
	}

	@Autowired
	@Qualifier("kotlinScriptFlow.input")
	private MessageChannel kotlinScriptFlowInput;

	@Test
	public void testKotlinScript() {
		this.kotlinScriptFlowInput.send(new GenericMessage<>(3));
		Message<?> receive = this.results.receive(10_000);
		assertThat(receive).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo(5);
	}


	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Value("scripts/TesSplitterScript.groovy")
		private Resource splitterScript;

		@Value("scripts/TestFilterScript.kts")
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
			return f -> f.route(Scripts.processor("scripts/TestRouterScript.py"));
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
					.from(Scripts.messageSource("scripts/TestMessageSourceScript.rb"),
							e -> e.poller(p -> p.fixedDelay(100)))
					.channel(c -> c.queue("messageSourceChannel"))
					.get();
		}

		@Bean
		public IntegrationFlow kotlinScriptFlow() {
			return f -> f
					.handle(Scripts.processor(new ByteArrayResource("2 + bindings[\"payload\"] as Int".getBytes()))
							.lang("kotlin"))
					.channel(results());
		}


	}

}
