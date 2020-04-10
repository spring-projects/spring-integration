/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.json.JsonPathUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.cloudevents.CloudEventHeaders;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeTypeUtils;

import io.cloudevents.CloudEvent;

/**
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class ToCloudEventTransformerTests {

	private static final ConfigurableApplicationContext APPLICATION_CONTEXT = TestUtils.createTestApplicationContext();

	private static final URI SOURCE = URI.create("https://spring.io/projects/spring-integration");

	@AfterAll
	static void teardown() {
		APPLICATION_CONTEXT.close();
	}

	@Test
	void testDefaultTransformer() {
		ToCloudEventTransformer transformer = new ToCloudEventTransformer(SOURCE);
		Message<?> result = transformer.transform(new GenericMessage<>("test"));
		assertThat(result.getHeaders()).containsOnlyKeys(MessageHeaders.ID, MessageHeaders.TIMESTAMP);
		assertThat(result.getPayload())
				.asInstanceOf(InstanceOfAssertFactories.type(CloudEvent.class))
				.satisfies(event -> {
							assertThat(event.getData().get()).isEqualTo("test");
							assertThat(event.getAttributes().getSource()).isEqualTo(SOURCE);
							assertThat(event.getAttributes().getSpecversion()).isEqualTo("1.0");
							assertThat(event.getAttributes().getType()).isEqualTo(String.class.getName());
							assertThat(event.getAttributes().getMediaType().isPresent()).isFalse();
						}
				);
	}

	@Test
	void testBinary() {
		ToCloudEventTransformer transformer =
				new ToCloudEventTransformer(SOURCE, ToCloudEventTransformer.Result.BINARY);
		transformer.setSubjectExpression(new LiteralExpression("some_subject"));
		Message<String> message =
				MessageBuilder.withPayload("test")
						.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
						.build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getHeaders())
				.containsEntry(CloudEventHeaders.TYPE, String.class.getName())
				.containsEntry(CloudEventHeaders.SOURCE, SOURCE.toString())
				.containsEntry(CloudEventHeaders.ID, message.getHeaders().getId().toString())
				.containsEntry(CloudEventHeaders.SUBJECT, "some_subject")
				.containsEntry(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE)
				.containsKeys(CloudEventHeaders.TIME, CloudEventHeaders.SPEC_VERSION)
				.doesNotContainKeys(
						CloudEventHeaders.DATA_CONTENT_TYPE,
						"ce_content_type");
		assertThat(result.getPayload())
				.isInstanceOf(byte[].class)
				.isEqualTo("test".getBytes());
	}

	@Test
	void testStructured() throws IOException {
		ToCloudEventTransformer transformer =
				new ToCloudEventTransformer(SOURCE, ToCloudEventTransformer.Result.STRUCTURED);
		GenericMessage<String> message = new GenericMessage<>("test");
		Message<?> result = transformer.transform(message);
		assertThat(result.getHeaders())
				.containsEntry(MessageHeaders.CONTENT_TYPE, "application/cloudevents+json")
				.doesNotContainKeys(
						CloudEventHeaders.ID,
						CloudEventHeaders.SOURCE,
						CloudEventHeaders.DATA_CONTENT_TYPE,
						CloudEventHeaders.TIME,
						CloudEventHeaders.SPEC_VERSION);
		Object payload = result.getPayload();
		assertThat(payload).isInstanceOf(byte[].class);

		List<?> jsonPath = JsonPathUtils.evaluate(payload, "$..data");
		assertThat(jsonPath.get(0)).isEqualTo("test");

		jsonPath = JsonPathUtils.evaluate(payload, "$..source");
		assertThat(jsonPath.get(0)).isEqualTo(SOURCE.toString());

		jsonPath = JsonPathUtils.evaluate(payload, "$..type");
		assertThat(jsonPath.get(0)).isEqualTo(String.class.getName());
	}

}
