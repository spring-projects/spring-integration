/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.json;

import static org.junit.Assert.assertThat;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.Test;

import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.MessageMatcher;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.json.JsonInboundMessageMapper;
import org.springframework.integration.support.json.JsonInboundMessageMapper.JsonMessageParser;
import org.springframework.integration.support.json.JsonOutboundMessageMapper;
import org.springframework.messaging.Message;


/**
 * @author Jeremy Grelle
 * @author Gary Russell
 */
public abstract class AbstractJsonSymmetricalMessageMappingTests {

	@Factory
	public static Matcher<Message<?>> sameExceptImmutableHeaders(Message<?> operand) {
		return new MessageMatcher(operand);
	}

	@Test
	public void testSymmetricalMappingWithHistory() throws Exception {
		Message<String> testMessage = MessageBuilder.withPayload("myPayloadStuff").build();
		testMessage = MessageHistory.write(testMessage, new TestNamedComponent(1));
		testMessage = MessageHistory.write(testMessage, new TestNamedComponent(2));
		testMessage = MessageHistory.write(testMessage, new TestNamedComponent(3));
		JsonOutboundMessageMapper outboundMapper = new JsonOutboundMessageMapper();

		String outboundJson = outboundMapper.fromMessage(testMessage);

		JsonInboundMessageMapper inboundMapper = new JsonInboundMessageMapper(String.class, getParser());
		Message<?> result = inboundMapper.toMessage(outboundJson);

		assertThat(result, sameExceptImmutableHeaders(testMessage));
	}

	protected abstract JsonMessageParser<?> getParser();

	private static class TestNamedComponent implements NamedComponent {

		private final int id;

		private TestNamedComponent(int id) {
			this.id = id;
		}

		public String getComponentName() {
			return "testName-" + this.id;
		}

		public String getComponentType() {
			return "testType-" + this.id;
		}

	}
}
