/*
 * Copyright 2002-2024 the original author or authors.
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
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.ContentEnricher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
public class EnricherParserWithRequestPayloadExpressionTests {

	@Autowired
	private ApplicationContext context;

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
		assertThat(accessor.getPropertyValue("shouldClonePayload")).isEqualTo(false);

		Expression requestPayloadExpression = (Expression) accessor.getPropertyValue("requestPayloadExpression");
		assertThat(requestPayloadExpression.getExpressionString()).isEqualTo("payload.age");

		Map<Expression, Expression> propertyExpressions =
				(Map<Expression, Expression>) accessor.getPropertyValue("propertyExpressions");
		for (Map.Entry<Expression, Expression> e : propertyExpressions.entrySet()) {
			if ("name".equals(e.getKey().getExpressionString())) {
				assertThat(e.getValue().getExpressionString()).isEqualTo("'Name as SpEL'");
			}
			else if ("age".equals(e.getKey().getExpressionString())) {
				assertThat(e.getValue().getExpressionString()).isEqualTo("payload.sourceName");
			}
			else {
				throw new IllegalStateException(
						"expected 'name' and 'age' only, not: " + e.getKey().getExpressionString());
			}
		}
	}

	@Test
	public void integrationTest() {
		SubscribableChannel requests = context.getBean("requests", SubscribableChannel.class);
		requests.subscribe(new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {

				assertThat(requestMessage.getPayload() instanceof Integer)
						.as("Expected the payload of the requestMessage to be a String").isTrue();

				Integer payload = (Integer) requestMessage.getPayload();
				assertThat(payload).as("Expected value: 99").isEqualTo(Integer.valueOf(99));

				return new Source(String.valueOf(payload));
			}
		});

		Target original = new Target();
		original.setAge(99);

		Message<?> request = MessageBuilder.withPayload(original).build();
		context.getBean("input", MessageChannel.class).send(request);
		Message<?> reply = context.getBean("output", PollableChannel.class).receive(0);
		Target enriched = (Target) reply.getPayload();
		assertThat(enriched.getName()).isEqualTo("Name as SpEL");
		assertThat(enriched.getAge()).isEqualTo(99);
		assertThat(enriched).isSameAs(original);
	}

	private record Source(String sourceName) {

	}

	public static class Target implements Cloneable {

		private volatile String name;

		private volatile int age;

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

		public Object clone() {
			Target copy = new Target();
			copy.setName(this.name);
			copy.setAge(this.age);
			return copy;
		}

	}

}
