/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.json;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public class JsonTransformersSymmetricalTests {

	@Test
	public void testInt2809ObjectToJson_JsonToObject() {

		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(new TestAddress(123, "Main Street"));

		List<TestPerson> payload = new ArrayList<>();
		payload.add(person);

		ObjectToJsonTransformer objectToJsonTransformer = new ObjectToJsonTransformer();
		Message<?> jsonMessage = objectToJsonTransformer.transform(new GenericMessage<Object>(payload));

		JsonToObjectTransformer jsonToObjectTransformer = new JsonToObjectTransformer();
		Object result = jsonToObjectTransformer.transform(jsonMessage).getPayload();
		assertThat(result).isInstanceOf(List.class);
		assertThat(((List<?>) result).get(0)).isEqualTo(person);
	}

}
