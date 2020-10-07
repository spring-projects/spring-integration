/*
 * Copyright 2002-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ResolvableType;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
public class JsonToObjectTransformerParserTests {

	@Autowired
	private volatile MessageChannel defaultObjectMapperInput;

	@Autowired
	private volatile MessageChannel customJsonObjectMapperInput;

	@Autowired
	@Qualifier("defaultJacksonMapperTransformer.handler")
	private MessageHandler defaultJacksonMapperTransformer;

	@Autowired
	@Qualifier("customJsonMapperTransformer.handler")
	private MessageHandler customJsonMapperTransformer;

	@Autowired
	private JsonObjectMapper<?, ?> jsonObjectMapper;

	@Test
	@SuppressWarnings("unchecked")
	public void testDefaultObjectMapper() {
		Object jsonToObjectTransformer =
				TestUtils.getPropertyValue(this.defaultJacksonMapperTransformer, "transformer");
		assertThat(TestUtils.getPropertyValue(jsonToObjectTransformer, "jsonObjectMapper").getClass())
				.isEqualTo(Jackson2JsonObjectMapper.class);

		DirectFieldAccessor dfa = new DirectFieldAccessor(jsonToObjectTransformer);
		LogAccessor logger = (LogAccessor) spy(dfa.getPropertyValue("logger"));
		dfa.setPropertyValue("logger", logger);

		String jsonString =
				"{\"firstName\":\"John\",\"lastName\":\"Doe\",\"age\":42," +
						"\"address\":{\"number\":123,\"street\":\"Main Street\"}}";
		QueueChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload(jsonString).setReplyChannel(replyChannel).build();
		this.defaultObjectMapperInput.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isNotNull();
		assertThat(reply.getPayload().getClass()).isEqualTo(TestPerson.class);
		TestPerson person = (TestPerson) reply.getPayload();
		assertThat(person.getFirstName()).isEqualTo("John");
		assertThat(person.getLastName()).isEqualTo("Doe");
		assertThat(person.getAge()).isEqualTo(42);
		assertThat(person.getAddress().toString()).isEqualTo("123 Main Street");

		ArgumentCaptor<Supplier<String>> argumentCaptor = ArgumentCaptor.forClass(Supplier.class);
		verify(logger).debug(any(Exception.class), argumentCaptor.capture());
		String logMessage = argumentCaptor.getValue().get();

		assertThat(logMessage).startsWith("Cannot build a ResolvableType from the request message");
	}

	@Test
	public void testInt2831CustomJsonObjectMapper() {
		Object jsonToObjectTransformer = TestUtils.getPropertyValue(this.customJsonMapperTransformer, "transformer");
		assertThat(TestUtils.getPropertyValue(jsonToObjectTransformer, "jsonObjectMapper", JsonObjectMapper.class))
				.isSameAs(this.jsonObjectMapper);

		String jsonString = "{firstName:'John', lastName:'Doe', age:42, address:{number:123, street:'Main Street'}}";
		QueueChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload(jsonString).setReplyChannel(replyChannel).build();
		this.customJsonObjectMapperInput.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isNotNull();
		assertThat(reply.getPayload().getClass()).isEqualTo(TestJsonContainer.class);
		TestJsonContainer result = (TestJsonContainer) reply.getPayload();
		assertThat(result.getJson()).isEqualTo(jsonString);
	}

	static class CustomJsonObjectMapper implements JsonObjectMapper<Object, Object> {

		@Override
		@SuppressWarnings("unchecked")
		public <T> T fromJson(Object json, Class<T> valueType) {
			return (T) new TestJsonContainer((String) json);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T fromJson(Object json, ResolvableType valueType) {
			return (T) new TestJsonContainer((String) json);
		}

	}

	static class TestJsonContainer {

		private final String json;

		TestJsonContainer(String json) {
			this.json = json;
		}

		public String getJson() {
			return json;
		}

	}

}
