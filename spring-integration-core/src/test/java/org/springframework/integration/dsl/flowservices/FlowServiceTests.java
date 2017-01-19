/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.integration.dsl.flowservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.CorrelationStrategy;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.ReleaseStrategy;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowAdapter;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.TriggerContext;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class FlowServiceTests {

	@Autowired
	@Qualifier("flowServiceTests.MyFlow.input")
	private MessageChannel input;

	@Autowired(required = false)
	private MyFlow myFlow;

	@Autowired
	private PollableChannel myFlowAdapterOutput;

	@Test
	public void testFlowServiceAndLogAsLastNoError() {
		assertNotNull(this.myFlow);
		this.input.send(MessageBuilder.withPayload("foo").build());
		Object result = this.myFlow.resultOverLoggingHandler.get();
		assertNotNull(result);
		assertEquals("FOO", result);
	}

	@Test
	public void testFlowAdapterService() {
		Message<?> receive = this.myFlowAdapterOutput.receive(10000);
		assertNotNull(receive);
		assertEquals("bar:FOO", receive.getPayload());
	}


	@Autowired
	@Qualifier("testGateway.input")
	private MessageChannel testGatewayInput;

	@Test
	public void testGatewayExplicitReplyChannel() {
		QueueChannel replyChannel = new QueueChannel();
		this.testGatewayInput.send(MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build());
		Message<?> message = replyChannel.receive(10000);
		assertNotNull(message);
		assertEquals("FOO", message.getPayload());
	}

	@Configuration
	@EnableIntegration
	@ComponentScan
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow testGateway() {
			return f -> f.gateway("processChannel", g -> g.replyChannel("replyChannel"))
					.log()
					.bridge(null);
		}

		@Bean
		public IntegrationFlow subFlow() {
			return IntegrationFlows
					.from("processChannel")
					.<String, String>transform(String::toUpperCase)
					.channel("replyChannel")
					.get();
		}

	}

	@Component
	public static class MyFlow implements IntegrationFlow {

		private final AtomicReference<Object> resultOverLoggingHandler = new AtomicReference<>();

		@Override
		public void configure(IntegrationFlowDefinition<?> f) {
			f.<String, String>transform(String::toUpperCase)
					.log(LoggingHandler.Level.ERROR, m -> {
						resultOverLoggingHandler.set(m.getPayload());
						return m;
					});
		}

	}

	@Component
	public static class MyFlowAdapter extends IntegrationFlowAdapter {

		private final AtomicReference<Date> executionDate = new AtomicReference<>(new Date());

		private Date nextExecutionTime(TriggerContext triggerContext) {
			return this.executionDate.getAndSet(null);
		}

		@Override
		protected IntegrationFlowDefinition<?> buildFlow() {
			return from(this, "messageSource", e -> e.poller(p -> p.trigger(this::nextExecutionTime)))
					.split(this, null, e -> e.applySequence(false))
					.transform(this)
					.aggregate(a -> a.processor(this, null))
					.enrichHeaders(Collections.singletonMap("foo", "FOO"))
					.filter(this)
					.handle(this)
					.channel(MessageChannels.queue("myFlowAdapterOutput"));
		}

		public String messageSource() {
			return "B,A,R";
		}

		@Splitter
		public String[] split(String payload) {
			return StringUtils.commaDelimitedListToStringArray(payload);
		}

		@Transformer
		public String transform(String payload) {
			return payload.toLowerCase();
		}


		@CorrelationStrategy
		public Integer correlationKey() {
			return 1;
		}

		@ReleaseStrategy
		public boolean canRelease(Collection<Message<?>> messages) {
			return messages.size() == 3;
		}

		@Aggregator
		public String aggregate(List<String> payloads) {
			return payloads.stream().collect(Collectors.joining());
		}

		@Filter
		public boolean filter(@Header Optional<String> foo) {
			return foo.isPresent();
		}

		@ServiceActivator
		public String handle(String payload, @Header String foo) {
			return payload + ":" + foo;
		}

	}

}
