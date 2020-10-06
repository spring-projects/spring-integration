/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.integration.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.test.condition.LogLevels;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.support.AvroHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @since 5.2
 *
 */
@SpringJUnitConfig
@LogLevels(categories = "foo", level = "DEBUG")
public class AvroTests {

	@Test
	@LogLevels(classes = DirectChannel.class, categories = "bar", level = "DEBUG")
	void testTransformers(@Autowired Config config) {
		AvroTestClass1 test = new AvroTestClass1("baz", "fiz");
		LogAccessor spied = spy(TestUtils.getPropertyValue(config.in1(), "logger", LogAccessor.class));
		new DirectFieldAccessor(config.in1()).setPropertyValue("logger", spied);
		config.in1().send(new GenericMessage<>(test));
		assertThat(config.tapped().receive(0))
				.isNotNull()
				.extracting(msg -> msg.getPayload())
				.isInstanceOf(byte[].class);
		Message<?> received = config.out().receive(0);
		assertThat(received)
				.isNotNull()
				.extracting(msg -> msg.getPayload())
				.isEqualTo(test)
				.isNotSameAs(test);
		assertThat(received.getHeaders().get("flow")).isEqualTo("flow1");
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(spied, atLeastOnce()).debug(captor.capture());
		assertThat(captor.getAllValues()).anyMatch(s -> s.contains("preSend on channel"));
		assertThat(captor.getAllValues()).anyMatch(s -> s.contains("postSend (sent=true) on channel"));
	}

	@Test
	void testMultiTypeTransformers(@Autowired Config config) {
		AvroTestClass1 test = new AvroTestClass1("baz", "fiz");
		config.in2().send(new GenericMessage<>(test));
		assertThat(config.tapped().receive(0))
				.isNotNull()
				.extracting(msg -> msg.getPayload())
				.isInstanceOf(byte[].class);
		Message<?> received = config.out().receive(0);
		assertThat(received)
				.isNotNull()
				.extracting(msg -> msg.getPayload())
				.isNotEqualTo(test)
				.isInstanceOf(AvroTestClass2.class);
		assertThat(received.getHeaders().get("flow")).isEqualTo("flow2");
	}

	@Test
	void testMultiTypeTransformersClassName(@Autowired Config config) {
		AvroTestClass1 test = new AvroTestClass1("baz", "fiz");
		config.in3().send(new GenericMessage<>(test));
		assertThat(config.tapped().receive(0))
				.isNotNull()
				.extracting(msg -> msg.getPayload())
				.isInstanceOf(byte[].class);
		Message<?> received = config.out().receive(0);
		assertThat(received)
				.isNotNull()
				.extracting(msg -> msg.getPayload())
				.isNotEqualTo(test)
				.isInstanceOf(AvroTestClass2.class);
		assertThat(received.getHeaders().get("flow")).isEqualTo("flow3");
	}

	@Test
	void testTransformersNoHeaderPresent(@Autowired Config config) {
		AvroTestClass1 test = new AvroTestClass1("baz", "fiz");
		config.in4().send(new GenericMessage<>(test));
		assertThat(config.tapped().receive(0))
				.isNotNull()
				.extracting(msg -> msg.getPayload())
				.isInstanceOf(byte[].class);
		Message<?> received = config.out().receive(0);
		assertThat(received)
				.isNotNull()
				.extracting(msg -> msg.getPayload())
				.isEqualTo(test)
				.isNotSameAs(test);
		assertThat(received.getHeaders().get("flow")).isEqualTo("flow4");
	}

	@Test
	void testTransformWithTypeMappingExpressions(@Autowired Config config) {
		AvroTestClass1 test = new AvroTestClass1("baz", "fiz");
		config.in5().send(new GenericMessage<>(test));
		assertThat(config.tapped().receive(0))
				.isNotNull()
				.extracting(msg -> msg.getPayload())
				.isInstanceOf(byte[].class);
		Message<?> received = config.out().receive(0);
		assertThat(received)
				.isNotNull()
				.extracting(msg -> msg.getPayload())
				.isNotEqualTo(test)
				.isInstanceOf(AvroTestClass2.class);
		assertThat(received.getHeaders().get("flow")).isEqualTo("flow5");
	}

	@Test
	void testTransformersFallbackWhenNoTypeMappingMatch(@Autowired Config config) {
		AvroTestClass1 test = new AvroTestClass1("baz", "fiz");
		config.in6().send(new GenericMessage<>(test));
		assertThat(config.tapped().receive(0))
				.isNotNull()
				.extracting(msg -> msg.getPayload())
				.isInstanceOf(byte[].class);
		Message<?> received = config.out().receive(0);
		assertThat(received)
				.isNotNull()
				.extracting(msg -> msg.getPayload())
				.isEqualTo(test)
				.isNotSameAs(test);
		assertThat(received.getHeaders().get("flow")).isEqualTo("flow6");
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public IntegrationFlow flow1() {
			return IntegrationFlows.from(in1())
					.transform(new SimpleToAvroTransformer())
					.wireTap(tapped())
					.transform(fromTransformer())
					.enrichHeaders(h -> h.header("flow", "flow1"))
					.channel(out())
					.get();
		}

		@Bean
		public IntegrationFlow flow2() {
			return IntegrationFlows.from(in2())
					.transform(new SimpleToAvroTransformer())
					.wireTap(tapped())
					.enrichHeaders(h -> h.header(AvroHeaders.TYPE, AvroTestClass2.class, true))
					.transform(fromTransformer())
					.enrichHeaders(h -> h.header("flow", "flow2"))
					.channel(out())
					.get();
		}

		@Bean
		public IntegrationFlow flow3() {
			return IntegrationFlows.from(in3())
					.transform(new SimpleToAvroTransformer())
					.wireTap(tapped())
					.enrichHeaders(h -> h.header(AvroHeaders.TYPE, AvroTestClass2.class.getName(), true))
					.transform(fromTransformer())
					.enrichHeaders(h -> h.header("flow", "flow3"))
					.channel(out())
					.get();
		}

		@Bean
		public IntegrationFlow flow4() {
			return IntegrationFlows.from(in4())
					.transform(new SimpleToAvroTransformer())
					.wireTap(tapped())
					.enrichHeaders(h -> h.header(AvroHeaders.TYPE, null, true)
							.shouldSkipNulls(false))
					.transform(fromTransformer())
					.enrichHeaders(h -> h.header("flow", "flow4"))
					.channel(out())
					.get();
		}

		@Bean
		public IntegrationFlow flow5() {
			return IntegrationFlows.from(in5())
					.transform(new SimpleToAvroTransformer().typeExpression("'avroTest'"))
					.wireTap(tapped())
					.transform(new SimpleFromAvroTransformer(AvroTestClass1.class)
							.typeExpression("'avroTest' == headers[avro_type] ? '"
									+ AvroTestClass2.class.getName() + "' : null"))
					.enrichHeaders(h -> h.header("flow", "flow5"))
					.channel(out())
					.get();
		}

		@Bean
		public IntegrationFlow flow6() {
			return IntegrationFlows.from(in6())
					.transform(new SimpleToAvroTransformer().typeExpression("'wontFindThisHeader'"))
					.wireTap(tapped())
					.transform(new SimpleFromAvroTransformer(AvroTestClass1.class)
							.typeExpression("'avroTest' == headers[avro_type] ? '"
									+ AvroTestClass2.class.getName() + "' : null"))
					.enrichHeaders(h -> h.header("flow", "flow6"))
					.channel(out())
					.get();
		}

		@Bean
		public SimpleFromAvroTransformer fromTransformer() {
			return new SimpleFromAvroTransformer(AvroTestClass1.class);
		}

		@Bean
		public DirectChannel in1() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel in2() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel in3() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel in4() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel in5() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel in6() {
			return new DirectChannel();
		}

		@Bean
		public PollableChannel tapped() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel out() {
			return new QueueChannel();
		}

	}

}
