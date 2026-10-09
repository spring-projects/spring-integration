/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.json;

import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.support.json.JacksonJsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @author Jooyoung Pyoung
 *
 * @since 2.0
 */
public class JsonToObjectTransformerTests {

	@Test
	public void objectPayload() {
		JsonToObjectTransformer transformer =
				new JsonToObjectTransformer(
						ResolvableType.forType(new ParameterizedTypeReference<List<TestPerson>>() {

						}));
		// Since DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES is disabled by default
		// the extra "foo" property is ignored.
		// language=JSON
		String jsonString = """
				[
					{
						"firstName": "John",
						"lastName": "Doe",
						"age": 42,
						"address": {
							"number": 123,
							"street": "Main Street"
						},
						"foo": "bar"
					}
				]""";
		Message<?> message = transformer.transform(new GenericMessage<>(jsonString));
		assertThat(message)
				.extracting(Message::getPayload)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.hasSize(1)
				.element(0)
				.isInstanceOf(TestPerson.class)
				.satisfies((actual) -> {
					TestPerson bean = (TestPerson) actual;
					assertThat(bean).extracting(TestPerson::getFirstName).isEqualTo("John");
					assertThat(bean).extracting(TestPerson::getLastName).isEqualTo("Doe");
					assertThat(bean).extracting(TestPerson::getAge).isEqualTo(42);
					assertThat(bean).extracting(TestPerson::getAddress).asString().isEqualTo("123 Main Street");
				});
	}

	@Test
	public void objectPayloadWithCustomMapper() {
		JsonMapper customMapper = JsonMapper.builder()
				.configure(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES, true)
				.configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true)
				.build();
		JsonToObjectTransformer transformer = new JsonToObjectTransformer(new JacksonJsonObjectMapper(customMapper));
		transformer.setValueTypeExpression(new ValueExpression<>(ResolvableType.forClass(TestPerson.class)));
		// language=JSON
		String jsonString = """
				{
					"firstName": "John",
					"lastName": "Doe",
					"age": 42,
					"address": {
						"number": 123,
						"street": "Main Street"
					}
				}""";
		Message<?> message = transformer.transform(new GenericMessage<>(jsonString));
		TestPerson person = (TestPerson) message.getPayload();
		assertThat(person.getFirstName()).isEqualTo("John");
		assertThat(person.getLastName()).isEqualTo("Doe");
		assertThat(person.getAge()).isEqualTo(42);
		assertThat(person.getAddress().toString()).isEqualTo("123 Main Street");
	}

}
