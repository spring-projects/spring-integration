/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.json;

import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
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
		// (see Jackson2JsonObjectMapper)
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
		ObjectMapper customMapper = new ObjectMapper();
		customMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, Boolean.TRUE);
		customMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, Boolean.TRUE);
		JsonToObjectTransformer transformer = new JsonToObjectTransformer(new Jackson2JsonObjectMapper(customMapper));
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
