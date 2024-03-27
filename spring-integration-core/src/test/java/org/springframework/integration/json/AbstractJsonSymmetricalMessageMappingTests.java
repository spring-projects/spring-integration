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

package org.springframework.integration.json;

import org.junit.Test;

import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.json.JsonInboundMessageMapper;
import org.springframework.integration.support.json.JsonInboundMessageMapper.JsonMessageParser;
import org.springframework.integration.support.json.JsonOutboundMessageMapper;
import org.springframework.integration.test.predicate.MessagePredicate;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jeremy Grelle
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractJsonSymmetricalMessageMappingTests {

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

		assertThat(result).matches(new MessagePredicate(testMessage));
	}

	protected abstract JsonMessageParser<?> getParser();

	private static class TestNamedComponent implements NamedComponent {

		private final int id;

		TestNamedComponent(int id) {
			this.id = id;
		}

		@Override
		public String getComponentName() {
			return "testName-" + this.id;
		}

		@Override
		public String getComponentType() {
			return "testType-" + this.id;
		}

	}

}
