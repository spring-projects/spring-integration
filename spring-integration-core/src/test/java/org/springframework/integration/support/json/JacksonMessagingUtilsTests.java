/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.support.json;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.kotlin.KotlinModule;

import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jooyoung Pyoung
 * @author Artem Bilan
 *
 * @since 7.0
 */
class JacksonMessagingUtilsTests {

	private JsonMapper mapper;

	@BeforeEach
	void setUp() {
		mapper = JacksonMessagingUtils.messagingAwareMapper();
	}

	@Test
	void shouldIncludeKotlinModule() {
		KotlinModule kotlinModule = new KotlinModule.Builder().build();
		Set<String> registeredModuleNames = getModuleNames(mapper.registeredModules().stream());
		assertThat(registeredModuleNames).contains(kotlinModule.getModuleName());
	}

	@Test
	void shouldSerializeAndDeserializeGenericMessage() {
		GenericMessage<String> originalMessage = new GenericMessage<>("Hello World");

		String json = mapper.writeValueAsString(originalMessage);
		GenericMessage<?> deserializedMessage = mapper.readValue(json, GenericMessage.class);

		assertThat(deserializedMessage).isNotNull();
		assertThat(deserializedMessage.getPayload()).isEqualTo("Hello World");
	}

	@Test
	void shouldSerializeAndDeserializeErrorMessage() {
		Exception exception = new RuntimeException("Test error");
		ErrorMessage originalMessage = new ErrorMessage(exception);

		String json = mapper.writeValueAsString(originalMessage);
		ErrorMessage deserializedMessage = mapper.readValue(json, ErrorMessage.class);

		assertThat(deserializedMessage).isNotNull();
		assertThat(deserializedMessage.getPayload()).isInstanceOf(Throwable.class);
	}

	@Test
	void shouldSerializeAndDeserializeAdviceMessage() {
		GenericMessage<String> originalMessage = new GenericMessage<>("Original");
		AdviceMessage<String> adviceMessage = new AdviceMessage<>("Advice payload", originalMessage);

		String json = mapper.writeValueAsString(adviceMessage);
		@SuppressWarnings("unchecked")
		AdviceMessage<String> deserializedMessage = mapper.readValue(json, AdviceMessage.class);

		assertThat(deserializedMessage).isNotNull();
		assertThat(deserializedMessage.getPayload()).isEqualTo("Advice payload");
	}

	@Test
	void shouldSerializeAndDeserializeMutableMessage() {
		MutableMessage<String> originalMessage = new MutableMessage<>("Mutable payload");

		String json = mapper.writeValueAsString(originalMessage);
		@SuppressWarnings("unchecked")
		MutableMessage<String> deserializedMessage = mapper.readValue(json, MutableMessage.class);

		assertThat(deserializedMessage).isNotNull();
		assertThat(deserializedMessage.getPayload()).isEqualTo("Mutable payload");
	}

	@Test
	void shouldSerializeMessageHeaders() {
		MessageHeaders headers = new MessageHeaders(null);

		String json = mapper.writeValueAsString(headers);

		assertThat(json).isNotNull();
		assertThat(json).contains("\"id\":");
		assertThat(json).contains("\"timestamp\":");
	}

	@Test
	void shouldSerializeMimeType() {
		MimeType mimeType = MimeType.valueOf("application/json");

		String json = mapper.writeValueAsString(mimeType);

		assertThat(json).isNotNull();
		assertThat(json).contains("application/json");
	}

	private static Set<String> getModuleNames(Stream<JacksonModule> modules) {
		return modules
				.map(JacksonModule::getModuleName)
				.collect(Collectors.toUnmodifiableSet());
	}

}
