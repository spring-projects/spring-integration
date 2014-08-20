/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.json;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.integration.support.json.BoonJsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class JsonTransformersSymmetricalTests {

	@Test
	public void testInt2809ObjectToJson_JsonToObject() {

		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(new TestAddress(123, "Main Street"));

		List<TestPerson> payload = new ArrayList<TestPerson>();
		payload.add(person);

		ObjectToJsonTransformer objectToJsonTransformer = new ObjectToJsonTransformer();
		Message<?> jsonMessage = objectToJsonTransformer.transform(new GenericMessage<Object>(payload));

		JsonToObjectTransformer jsonToObjectTransformer = new JsonToObjectTransformer();
		Object result = jsonToObjectTransformer.transform(jsonMessage).getPayload();
		assertThat(result, Matchers.instanceOf(List.class));
		assertEquals(person, ((List) result).get(0));
	}

	@Test
	public void testBoonObjectToJson_JsonToObject() {

		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(new TestAddress(123, "Main Street"));

		List<TestPerson> payload = new ArrayList<TestPerson>();
		payload.add(person);

		ObjectToJsonTransformer objectToJsonTransformer = new ObjectToJsonTransformer(new BoonJsonObjectMapper());
		Message<?> jsonMessage = objectToJsonTransformer.transform(new GenericMessage<Object>(payload));

		JsonToObjectTransformer jsonToObjectTransformer = new JsonToObjectTransformer(new BoonJsonObjectMapper());
		Object result = jsonToObjectTransformer.transform(jsonMessage).getPayload();
		assertThat(result, Matchers.instanceOf(List.class));
		assertEquals(person, ((List) result).get(0));
	}


}
