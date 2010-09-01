/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * @author Dave Syer
 * 
 */
public class ExpressionEvaluatingSqlParameterSourceFactoryTests {

	private ExpressionEvaluatingSqlParameterSourceFactory factory = new ExpressionEvaluatingSqlParameterSourceFactory();

	@Test
	public void testSetStaticParameters() {
		factory.setStaticParameters(Collections.singletonMap("foo", "bar"));
		SqlParameterSource source = factory.createParameterSource(null);
		assertTrue(source.hasValue("foo"));
		assertEquals("bar", source.getValue("foo"));
	}

	@Test
	public void testMapInput() {
		SqlParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		assertTrue(source.hasValue("foo"));
		assertEquals("bar", source.getValue("foo"));
	}

	@Test
	public void testListOfMapsInput() {
		@SuppressWarnings("unchecked")
		SqlParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "foo";
		assertTrue(source.hasValue(expression));
		assertEquals("[bar, bucket]", source.getValue(expression).toString());
	}

	@Test
	public void testMapInputWithExpression() {
		SqlParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		assertTrue(source.hasValue("foo.toUpperCase()"));
		assertEquals("BAR", source.getValue("foo.toUpperCase()"));
	}

}
