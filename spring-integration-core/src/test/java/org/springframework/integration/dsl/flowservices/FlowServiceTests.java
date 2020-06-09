/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.dsl.flowservices;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
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
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.TriggerContext;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class FlowServiceTests {

	@Autowired(required = false)
	@Qualifier("flowServiceTests.MyFlow")
	private IntegrationFlow myFlow;

	@Autowired
	@Qualifier("flowServiceTests.MyFlow.input")
	private MessageChannel input;

	@Autowired
	private PollableChannel myFlowAdapterOutput;

	@Test
	public void testFlowServiceAndLogAsLastNoError() throws Exception {
		assertThat(this.myFlow).isNotNull();
		assertThat(AopUtils.isAopProxy(this.myFlow)).isTrue();
		assertThat(this.myFlow).isInstanceOf(Advised.class);
		assertThat(this.myFlow).isInstanceOf(Ordered.class);
		assertThat(this.myFlow).isInstanceOf(SmartLifecycle.class);

		this.input.send(MessageBuilder.withPayload("foo").build());

		MyFlow myFlow = (MyFlow) ((Advised) this.myFlow).getTargetSource().getTarget();
		Object result = myFlow.resultOverLoggingHandler.get();
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("FOO");
	}

	@Test
	public void testFlowAdapterService() {
		Message<?> receive = this.myFlowAdapterOutput.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("bar:FOO");
	}


	@Autowired
	@Qualifier("testGateway.input")
	private MessageChannel testGatewayInput;

	@Test
	public void testGatewayExplicitReplyChannel() {
		QueueChannel replyChannel = new QueueChannel();
		this.testGatewayInput.send(MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build());
		Message<?> message = replyChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("FOO");
	}

	@Configuration
	@EnableIntegration
	@ComponentScan
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow testGateway() {
			return f -> f.gateway("processChannel", g -> g.replyChannel("replyChannel"))
					.logAndReply();
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
	public static class MyFlow implements IntegrationFlow, Ordered {

		private final AtomicReference<Object> resultOverLoggingHandler = new AtomicReference<>();

		@Override
		public void configure(IntegrationFlowDefinition<?> f) {
			f.<String, String>transform(String::toUpperCase)
					.log(LoggingHandler.Level.ERROR, m -> {
						resultOverLoggingHandler.set(m.getPayload());
						return m;
					});
		}

		@Override
		public int getOrder() {
			return 0;
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
			return fromSupplier(this::messageSource, e -> e.poller(p -> p.trigger(this::nextExecutionTime)))
					.split(this, null, e -> e.applySequence(false))
					.transform(this)
					.aggregate(a -> a.processor(this, null))
					.enrichHeaders(Collections.singletonMap("foo", "FOO"))
					.filter(this)
					.handle(this)
					.channel(MessageChannels.queue("myFlowAdapterOutput"))
					.log();
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
			return String.join("", payloads);
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
