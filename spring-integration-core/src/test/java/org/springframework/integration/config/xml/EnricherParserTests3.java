/*
 * Copyright 2002-2012 the original author or authors.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 *
 * @since 2.1.1
 */
public class EnricherParserTests3 {

	@Test
	public void testSourceBeanResolver() {
		ApplicationContext context = new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-context.xml", this.getClass());
		MessageChannel beanResolveIn = context.getBean("beanResolveIn", MessageChannel.class);
		PollableChannel beanResolveOut = context.getBean("beanResolveOut", PollableChannel.class);
		SomeBean payload = new SomeBean("foo");
		assertEquals("foo", payload.getNested().getValue());
		beanResolveIn.send(new GenericMessage<SomeBean>(payload));
		@SuppressWarnings("unchecked")
		Message<SomeBean> out =  (Message<SomeBean>) beanResolveOut.receive();
		assertSame(payload, out.getPayload());
		assertEquals("bar", out.getPayload().getNested().getValue());
	}

	@Test
	public void testTargetBeanResolver() {
		ApplicationContext context = new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail-context.xml", this.getClass());
		MessageChannel beanResolveIn = context.getBean("beanResolveIn", MessageChannel.class);
		SomeBean payload = new SomeBean("foo");
		assertEquals("foo", payload.getNested().getValue());
		try {
			beanResolveIn.send(new GenericMessage<SomeBean>(payload));
			fail("Expected SpEL Exception");
		}
		catch (MessageHandlingException e) {
			assertTrue(e.getCause() instanceof SpelEvaluationException);
		}
	}

	public static class SomeBean {

		private Nested nested = new Nested();

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
