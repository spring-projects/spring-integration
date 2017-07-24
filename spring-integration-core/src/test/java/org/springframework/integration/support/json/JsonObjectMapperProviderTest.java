/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.support.json;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Vikas Prasad
 * @since 5.0.0
 */
public class JsonObjectMapperProviderTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getCustomizedJacksonMapper_PassObjectMapper_ReturnsJackson2JsonObjectMapperObject() {
		// arrange + act
		JsonObjectMapper<?, ?> mapper = JsonObjectMapperProvider.getCustomizedJacksonMapper(new ObjectMapper());

		// assert
		Assert.assertNotNull("The method should return an instance of JsonObjectMapper", mapper);
	}

	@Test
	public void getCustomizedJacksonMapper_PassNull_ThrowsException() {
		this.expectedException.expect(IllegalArgumentException.class);
		JsonObjectMapperProvider.getCustomizedJacksonMapper(null);
	}

}
