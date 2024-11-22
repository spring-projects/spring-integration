/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.channel;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class FixedSubscriberChannelTests {

	@Autowired
	private MessageChannel in;

	@Autowired
	private PollableChannel out;

	@Test
	public void testHappyDay() {
		this.in.send(new GenericMessage<>("test"));
		Message<?> out = this.out.receive(0);
		assertThat(out.getPayload()).isEqualTo("TEST");
		assertThat(this.in).isInstanceOf(FixedSubscriberChannel.class);
	}

	@Test
	public void testNoSubs() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "NoSubs-fail-context.xml",
						this.getClass()))
				.withCauseInstanceOf(BeanInstantiationException.class)
				.withRootCauseInstanceOf(IllegalArgumentException.class)
				.withStackTraceContaining("Cannot instantiate a");
	}

	@Test
	public void testTwoSubs() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "TwoSubs-fail-context.xml",
						this.getClass()))
				.withMessageContaining("Only one subscriber is allowed for a FixedSubscriberChannel.");
	}

	@Test
	public void testTwoSubsAfter() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "TwoSubs-fail-context.xml",
						this.getClass()))
				.withMessageContaining("Only one subscriber is allowed for a FixedSubscriberChannel.");
	}

	@Test
	public void testInterceptors() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "Interceptors-fail-context.xml",
						this.getClass()))
				.withMessageContaining("Cannot have interceptors when 'fixed-subscriber=\"true\"'");
	}

	@Test
	public void testDatatype() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "Datatype-fail-context.xml",
						this.getClass()))
				.withMessageContaining("Cannot have 'datatype' when 'fixed-subscriber=\"true\"'");
	}

	@Test
	public void testConverter() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "Converter-fail-context.xml",
						this.getClass()))
				.withMessageContaining("Cannot have 'message-converter' when 'fixed-subscriber=\"true\"'");
	}

	@Test
	public void testQueue() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "Queue-fail-context.xml",
						this.getClass()))
				.withMessageContaining("The 'fixed-subscriber' attribute is not allowed when a <queue/> child element is present.");
	}

	@Test
	public void testDispatcher() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "Dispatcher-fail-context.xml",
						this.getClass()))
				.withMessageContaining("The 'fixed-subscriber' attribute is not allowed when a <dispatcher/> child element is present.");
	}

}
