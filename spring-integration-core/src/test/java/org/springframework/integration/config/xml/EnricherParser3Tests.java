/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Gary Russell
 *
 * @since 2.1.1
 */
public class EnricherParser3Tests {

	@Test
	public void testSourceBeanResolver() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				this.getClass().getSimpleName() + "-context.xml", this.getClass());
		MessageChannel beanResolveIn = context.getBean("beanResolveIn", MessageChannel.class);
		PollableChannel beanResolveOut = context.getBean("beanResolveOut", PollableChannel.class);
		SomeBean payload = new SomeBean("foo");
		assertThat(payload.getNested().getValue()).isEqualTo("foo");
		beanResolveIn.send(new GenericMessage<>(payload));
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
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> beanResolveIn.send(new GenericMessage<>(payload)))
				.withCauseInstanceOf(SpelEvaluationException.class);
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

		public static class Nested {

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
