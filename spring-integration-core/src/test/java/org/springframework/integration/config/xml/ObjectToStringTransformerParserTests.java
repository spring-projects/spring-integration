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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class ObjectToStringTransformerParserTests {

	@Autowired
	@Qualifier("directInput")
	private MessageChannel directInput;

	@Autowired
	@Qualifier("queueInput")
	private MessageChannel queueInput;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	private AbstractEndpoint withCharset;

	@Test
	public void directChannelWithStringMessage() {
		directInput.send(new GenericMessage<String>("foo"));
		Message<?> result = output.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("foo");
	}

	@Test
	public void queueChannelWithStringMessage() {
		queueInput.send(new GenericMessage<String>("foo"));
		Message<?> result = output.receive(3000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("foo");
	}

	@Test
	public void directChannelWithObjectMessage() {
		directInput.send(new GenericMessage<TestBean>(new TestBean()));
		Message<?> result = output.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test");
	}

	@Test
	public void queueChannelWithObjectMessage() {
		queueInput.send(new GenericMessage<TestBean>(new TestBean()));
		Message<?> result = output.receive(3000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test");
	}

	@Test
	public void charset() {
		assertThat(TestUtils.<String>getPropertyValue(this.withCharset, "handler.transformer.charset"))
				.isEqualTo("FOO");
	}

	private static class TestBean {

		TestBean() {
			super();
		}

		@Override
		public String toString() {
			return "test";
		}

	}

}
