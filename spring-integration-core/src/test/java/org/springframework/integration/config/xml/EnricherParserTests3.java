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

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 *
 * @since 2.1.1
 */
public class EnricherParserTests3 {

	@Test
	public void testSourceBeanResolver() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				this.getClass().getSimpleName() + "-context.xml", this.getClass());
		MessageChannel beanResolveIn = context.getBean("beanResolveIn", MessageChannel.class);
		PollableChannel beanResolveOut = context.getBean("beanResolveOut", PollableChannel.class);
		SomeBean payload = new SomeBean("foo");
		assertThat(payload.getNested().getValue()).isEqualTo("foo");
		beanResolveIn.send(new GenericMessage<SomeBean>(payload));
		@SuppressWarnings("unchecked")
		Message<SomeBean> out = (Message<SomeBean>) beanResolveOut.receive();
		assertThat(out.getPayload()).isSameAs(payload);
		assertThat(out.getPayload().getNested().getValue()).isEqualTo("bar");
		context.close();
	}

	@Test
	public void testTargetBeanResolver() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				this.getClass().getSimpleName() + "-fail-context.xml", this.getClass());
		MessageChannel beanResolveIn = context.getBean("beanResolveIn", MessageChannel.class);
		SomeBean payload = new SomeBean("foo");
		assertThat(payload.getNested().getValue()).isEqualTo("foo");
		try {
			beanResolveIn.send(new GenericMessage<SomeBean>(payload));
			fail("Expected SpEL Exception");
		}
		catch (MessageHandlingException e) {
			assertThat(e.getCause() instanceof SpelEvaluationException).isTrue();
		}
		context.close();
	}

	public static class SomeBean {

		private final Nested nested = new Nested();

		public SomeBean(String someProperty) {
			this.nested.setValue(someProperty);
		}

		public Nested getNested() {
			return nested;
		}

		public String getSomeOtherProperty() {
			return "bar";
		}

		public class Nested {

			private String value;

			public void setValue(String value) {
				this.value = value;
			}

			public String getValue() {
				return value;
			}

		}

	}

}
