/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.ContentEnricher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
public class EnricherParserTests {

	@Autowired
	private ApplicationContext context;

	private static volatile int adviceCalled;

	@Test
	@SuppressWarnings("unchecked")
	public void configurationCheck() {
		Object endpoint = context.getBean("enricher");
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object handler = TestUtils.getPropertyValue(endpoint, "handler");
		assertThat(handler.getClass()).isEqualTo(ContentEnricher.class);
		ContentEnricher enricher = (ContentEnricher) handler;
		assertThat(enricher.getOrder()).isEqualTo(99);
		DirectFieldAccessor accessor = new DirectFieldAccessor(enricher);
		assertThat(accessor.getPropertyValue("outputChannelName")).isEqualTo("output");
		assertThat(accessor.getPropertyValue("shouldClonePayload")).isEqualTo(true);
		assertThat(accessor.getPropertyValue("requestPayloadExpression")).isNull();
		assertThat(TestUtils.<Object>getPropertyValue(enricher, "gateway.beanFactory")).isNotNull();

		Map<Expression, Expression> propertyExpressions =
				(Map<Expression, Expression>) accessor.getPropertyValue("propertyExpressions");
		for (Map.Entry<Expression, Expression> e : propertyExpressions.entrySet()) {
			if ("name".equals(e.getKey().getExpressionString())) {
				assertThat(e.getValue().getExpressionString()).isEqualTo("payload.sourceName");
			}
			else if ("age".equals(e.getKey().getExpressionString())) {
				assertThat(e.getValue().getExpressionString()).isEqualTo("42");
			}
			else if ("gender".equals(e.getKey().getExpressionString())) {
				assertThat(e.getValue().getExpressionString()).isEqualTo(Gender.MALE.name());
			}
			else if ("married".equals(e.getKey().getExpressionString())) {
				assertThat(e.getValue().getExpressionString()).isEqualTo(Boolean.TRUE.toString());
			}
			else {
				throw new IllegalStateException("expected 'name', 'age', 'gender' and married only, not: "
						+ e.getKey().getExpressionString());
			}
		}

	}

	@Test
	public void configurationCheckTimeoutParameters() {
		Object endpoint = context.getBean("enricher");

		Long requestTimeout = TestUtils.getPropertyValue(endpoint, "handler.requestTimeout");
		Long replyTimeout = TestUtils.getPropertyValue(endpoint, "handler.replyTimeout");

		assertThat(requestTimeout).isEqualTo(Long.valueOf(1234L));
		assertThat(replyTimeout).isEqualTo(Long.valueOf(9876L));
	}

	@Test
	public void configurationCheckRequiresReply() {
		Object endpoint = context.getBean("enricher");

		boolean requiresReply = TestUtils.<Boolean>getPropertyValue(endpoint, "handler.requiresReply");

		assertThat(requiresReply).as("Was expecting requiresReply to be 'false'").isTrue();
	}

	@Test
	public void integrationTest() {
		QueueChannel output = context.getBean("output", QueueChannel.class);
		output.purge(null);

		SubscribableChannel requests = context.getBean("requests", SubscribableChannel.class);

		class Foo extends AbstractReplyProducingMessageHandler {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new Source("foo");
			}

		}

		Foo foo = new Foo();
		foo.setOutputChannel(context.getBean("replies", MessageChannel.class));
		requests.subscribe(foo);
		Target original = new Target();
		Message<?> request = MessageBuilder.withPayload(original)
				.setHeader("sourceName", "test")
				.setHeader("notOverwrite", "test")
				.build();
		context.getBean("input", MessageChannel.class).send(request);

		Message<?> reply = output.receive(0);
		Target enriched = (Target) reply.getPayload();
		assertThat(enriched.getName()).isEqualTo("foo");
		assertThat(enriched.getAge()).isEqualTo(42);
		assertThat(enriched.getGender()).isEqualTo(Gender.MALE);
		assertThat(enriched.isMarried()).isTrue();
		assertThat(enriched).isNotSameAs(original);
		assertThat(adviceCalled).isEqualTo(1);

		MessageHeaders headers = reply.getHeaders();
		assertThat(headers.get("foo")).isEqualTo("bar");
		assertThat(headers.get("testBean")).isEqualTo(Gender.MALE);
		assertThat(headers.get("sourceName")).isEqualTo("foo");
		assertThat(headers.get("notOverwrite")).isEqualTo("test");
		requests.unsubscribe(foo);
		adviceCalled--;
	}

	private record Source(String sourceName) {

	}

	public static class Target implements Cloneable {

		private volatile String name;

		private volatile int age;

		private volatile Gender gender;

		private volatile boolean married;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}

		public boolean isMarried() {
			return married;
		}

		public void setMarried(boolean married) {
			this.married = married;
		}

		@Override
		public Object clone() {
			Target copy = new Target();
			copy.setName(this.name);
			copy.setAge(this.age);
			copy.setGender(this.gender);
			copy.setMarried(this.married);
			return copy;
		}

	}

	public enum Gender {
		MALE, FEMALE

	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

}
