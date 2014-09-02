/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.integration.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for {@link JsonPropertyAccessor}.
 *
 * @author Eric Bottard
 * @author Artem Bilan
 * @since 3.0
 */
public class JsonPropertyAccessorTests {

	private final SpelExpressionParser parser = new SpelExpressionParser();

	private final StandardEvaluationContext context = new StandardEvaluationContext();

	private final ObjectMapper mapper = new ObjectMapper();

	@Before
	public void setup() {
		context.addPropertyAccessor(new JsonPropertyAccessor());
	}

	@Test
	public void testSimpleLookup() throws Exception {
		Object json = mapper.readTree("{\"foo\": \"bar\"}");
		Object value = evaluate(json, "foo", Object.class);
		assertThat(value, Matchers.instanceOf(JsonPropertyAccessor.ToStringFriendlyJsonNode.class));
		assertEquals("bar", value.toString());
		Object json2 = mapper.readTree("{\"foo\": \"bar\"}");
		Object value2 = evaluate(json2, "foo", Object.class);
		assertThat(value2, Matchers.instanceOf(JsonPropertyAccessor.ToStringFriendlyJsonNode.class));
		assertTrue(value.equals(value2));
		assertEquals(value.hashCode(), value2.hashCode());
	}

	@Test(expected = SpelEvaluationException.class)
	public void testUnsupportedJsonConstruct() throws Exception {
		Object json = mapper.readTree("\"foo\"");
		evaluate(json, "fizz", Object.class);
	}

	@Test(expected = SpelEvaluationException.class)
	public void testMissingProperty() throws Exception {
		Object json = mapper.readTree("{\"foo\": \"bar\"}");
		evaluate(json, "fizz", Object.class);
	}

	@Test
	public void testArrayLookup() throws Exception {
		Object json = mapper.readTree("[3, 4, 5]");
		// JsonNode actual = evaluate("1", json, JsonNode.class); // Does not work
		// JsonNode actual = evaluate("'1'", json, JsonNode.class); // Does not work
		Object actual = evaluate(json, "['1']", Object.class);
		assertEquals("4", actual.toString());
	}

	@Test
	public void testNestedArrayConstruct() throws Exception {
		Object json = mapper.readTree("[[3], [4, 5], []]");
		// JsonNode actual = evaluate("1.1", json, JsonNode.class); // Does not work
		// JsonNode actual = evaluate("[1][1]", json, JsonNode.class); // Does not work
		Object actual = evaluate(json, "['1']['1']", Object.class);
		assertEquals("5", actual.toString());
	}

	@Test
	public void testNestedHashConstruct() throws Exception {
		Object json = mapper.readTree("{\"foo\": {\"bar\": 4, \"fizz\": 5} }");
		Object actual = evaluate(json, "foo.fizz", Object.class);
		assertEquals("5", actual.toString());
	}

	@Test
	public void testImplicitStringConversion() throws Exception {
		String json = "{\"foo\": {\"bar\": 4, \"fizz\": 5} }";
		Object actual = evaluate(json, "foo.fizz", Object.class);
		assertEquals("5", actual.toString());
	}

	@Test(expected = SpelEvaluationException.class)
	public void testUnsupportedString() throws Exception {
		String xml = "<what>?</what>";
		evaluate(xml, "what", Object.class);
	}

	@Test(expected = SpelEvaluationException.class)
	public void testUnsupportedJson() throws Exception {
		String json = "\"literal\"";
		evaluate(json, "foo", Object.class);
	}

	private <T> T evaluate(Object target, String expression, Class<T> expectedType) {
		return parser.parseExpression(expression).getValue(context, target, expectedType);
	}

}
