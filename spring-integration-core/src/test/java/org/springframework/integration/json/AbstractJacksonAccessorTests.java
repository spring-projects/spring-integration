/*
 * Copyright 2013-present the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.integration.json.JacksonPropertyAccessor.ArrayNodeAsList;
import org.springframework.integration.json.JacksonPropertyAccessor.ComparableJsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Abstract base class for tests involving {@link JacksonPropertyAccessor} and {@link JacksonIndexAccessor}.
 *
 * @author Eric Bottard
 * @author Artem Bilan
 * @author Paul Martin
 * @author Pierre Lakreb
 * @author Sam Brannen
 * @author Jooyoung Pyoung
 *
 * @since 3.0
 */
public abstract class AbstractJacksonAccessorTests {

	protected final SpelExpressionParser parser = new SpelExpressionParser();

	protected final JsonMapper mapper = new JsonMapper();

	protected final StandardEvaluationContext context = new StandardEvaluationContext();

	@BeforeEach
	void setUpEvaluationContext() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new JsonNodeWrapperConverter());
		context.setTypeConverter(new StandardTypeConverter(conversionService));
	}

	/**
	 * Tests for JSON accessors that use an instance of {@link JsonNode} as the
	 * root context object.
	 */
	@Nested
	class JsonNodeTests {

		@Test
		void textNode() {
			StringNode json = (StringNode) mapper.readTree("\"foo\"");
			String result = evaluate(json, "#root", String.class);
			assertThat(result).isEqualTo("\"foo\"");
		}

		@Test
		void nullProperty() {
			JsonNode json = mapper.readTree("{\"foo\": null}");
			assertThat(evaluate(json, "foo", String.class)).isNull();
		}

		@Test
		void missingProperty() {
			JsonNode json = mapper.readTree(FOO_BAR_JSON);
			assertThat(evaluate(json, "fizz", String.class)).isNull();
		}

		@Test
		void propertyLookup() {
			JsonNode json1 = mapper.readTree(FOO_BAR_JSON);
			String value1 = evaluate(json1, "foo", String.class);
			assertThat(value1).isEqualTo("bar");
			JsonNode json2 = mapper.readTree(FOO_BAR_JSON);
			String value2 = evaluate(json2, "foo", String.class);
			assertThat(value1).isEqualTo(value2).hasSameHashCodeAs(value2);
		}

		@Test
		void arrayLookupWithIntegerIndexAndExplicitWrapping() throws Exception {
			ArrayNode json = (ArrayNode) mapper.readTree("[3, 4, 5]");
			// Have to wrap the root array because ArrayNode itself is not a List
			Integer actual = evaluate(JacksonPropertyAccessor.wrap(json), "[1]", Integer.class);
			assertThat(actual).isEqualTo(4);
		}

		@Test
		void arrayLookupWithIntegerIndexForNullValueAndExplicitWrapping() throws Exception {
			ArrayNode json = (ArrayNode) mapper.readTree("[3, null, 5]");
			// Have to wrap the root array because ArrayNode itself is not a List
			Integer actual = evaluate(JacksonPropertyAccessor.wrap(json), "[1]", Integer.class);
			assertThat(actual).isNull();
		}

		@Test
		void arrayLookupWithNegativeIntegerIndex() {
			JsonNode json = mapper.readTree("{\"foo\": [3, 4, 5]}");
			// ArrayNodeAsList allows one to index into a JSON array via a negative index.
			assertThat(evaluate(json, "foo[-1]", Integer.class)).isEqualTo(5);
		}

		@Test
		void arrayLookupWithNegativeIntegerIndexGreaterThanArrayLength() {
			JsonNode json = mapper.readTree("{\"foo\": [3, 4, 5]}");
			// Although ArrayNodeAsList allows one to index into a JSON array via a negative
			// index, if the result of (array.length - index) is still negative, Jackson's
			// ArrayNode.get() method returns null instead of throwing an IndexOutOfBoundsException.
			assertThat(evaluate(json, "foo[-99]", Integer.class)).isNull();
		}

		@Test
		void arrayLookupWithNegativeIntegerIndexForNullValue() {
			JsonNode json = mapper.readTree("{\"foo\": [3, 4, null]}");
			// ArrayNodeAsList allows one to index into a JSON array via a negative index.
			assertThat(evaluate(json, "foo[-1]", Integer.class)).isNull();
		}

		@Test
		void arrayLookupWithIntegerIndexOutOfBounds() {
			JsonNode json = mapper.readTree("{\"foo\": [3, 4, 5]}");
			assertThatExceptionOfType(SpelEvaluationException.class)
					.isThrownBy(() -> evaluate(json, "foo[3]", Object.class))
					.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.COLLECTION_INDEX_OUT_OF_BOUNDS));
		}

		@Test
		void arrayLookupWithStringIndex() {
			JsonNode json = mapper.readTree("[3, 4, 5]");
			Integer actual = evaluate(json, "['1']", Integer.class);
			assertThat(actual).isEqualTo(4);
		}

		@Test
		void nestedArrayLookupWithIntegerIndexAndExplicitWrapping() throws Exception {
			ArrayNode json = (ArrayNode) mapper.readTree("[[3], [4, 5], []]");
			Object actual = evaluate(JacksonPropertyAccessor.wrap(json), "[1][1]", Object.class);
			assertThat(actual).isEqualTo(5);
		}

		@Test
		void nestedArrayLookupWithStringIndex() {
			JsonNode json = mapper.readTree("[[3], [4, 5], []]");
			Integer actual = evaluate(json, "['1']['1']", Integer.class);
			assertThat(actual).isEqualTo(5);
		}

		@Test
		@SuppressWarnings("unchecked")
		void nestedArrayLookupWithStringIndexAndThenIntegerIndex() {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[[3], [4, 5], []]");

			List<Integer> list = evaluate(arrayNode, "['0']", List.class);
			assertThat(list).isInstanceOf(ArrayNodeAsList.class).containsExactly(3);
			list = evaluate(arrayNode, "['2']", List.class);
			assertThat(list).isInstanceOf(ArrayNodeAsList.class).isEmpty();

			Integer number = evaluate(arrayNode, "['0'][0]", Integer.class);
			assertThat(number).isEqualTo(3);
			number = evaluate(arrayNode, "['1'][1]", Integer.class);
			assertThat(number).isEqualTo(5);
		}

		@Test
		void arrayProjection() {
			JsonNode json = mapper.readTree(FOO_BAR_ARRAY_FIZZ_JSON);

			// Filter the bar array to return only the fizz value of each element (to prove that SpEL considers bar
			// an array/list)
			List<?> actualArray = evaluate(json, "foo.bar.![fizz]", List.class);
			assertThat(actualArray).hasSize(3);
			assertThat(evaluate(actualArray, "[0]", Object.class)).isEqualTo(5);
			assertThat(evaluate(actualArray, "[1]", Object.class)).isEqualTo(7);
			assertThat(evaluate(actualArray, "[2]", Object.class)).isEqualTo(8);
		}

		@Test
		void arraySelection() {
			JsonNode json = mapper.readTree(FOO_BAR_ARRAY_FIZZ_JSON);

			// Filter bar objects so that none match
			List<?> actualArray = evaluate(json, "foo.bar.?[fizz == 0]", List.class);
			assertThat(actualArray).isEmpty();

			// Filter bar objects so that one match
			actualArray = evaluate(json, "foo.bar.?[fizz == 8]", List.class);
			assertThat(actualArray).hasSize(1);
			assertThat(((ComparableJsonNode) actualArray.get(0)).getRealNode()).isEqualTo(mapper.readTree("{\"fizz\": 8}"));

			// Filter bar objects so several match
			actualArray = evaluate(json, "foo.bar.?[fizz > 6]", List.class);
			assertThat(actualArray).hasSize(2);
			assertThat(((ComparableJsonNode) actualArray.get(0)).getRealNode()).isEqualTo(mapper.readTree("{\"fizz\": 7}"));
			assertThat(((ComparableJsonNode) actualArray.get(1)).getRealNode()).isEqualTo(mapper.readTree("{\"fizz\": 8}"));
		}

		@Test
		void nestedPropertyAccessViaJsonNode() {
			JsonNode json = mapper.readTree(FOO_BAR_FIZZ_JSON);

			assertThat(evaluate(json, "foo.bar", Integer.class)).isEqualTo(4);
			assertThat(evaluate(json, "foo.fizz", Integer.class)).isEqualTo(5);
		}

		@Test
		void noNullPointerExceptionWithCachedReadAccessor() {
			Expression expression = parser.parseExpression("foo");
			JsonNode json1 = mapper.readTree(FOO_BAR_JSON);
			String value1 = expression.getValue(context, json1, String.class);
			assertThat(value1).isEqualTo("bar");
			JsonNode json2 = mapper.readTree("{}");
			Object value2 = expression.getValue(context, json2);
			assertThat(value2).isNull();
		}

	}

	/**
	 * Tests for JSON accessors that use a String-representation of a JSON document
	 * as the root context object.
	 */
	@Nested
	class JsonAsStringTests {

		@Test
		void selectorAccess() {
			String actual = evaluate(PROPERTY_NAMES_JSON, "property.^[name == 'value1'].name", String.class);
			assertThat(actual).isEqualTo("value1");
		}

		@Test
		void nestedPropertyAccessViaJsonAsString() {
			String json = FOO_BAR_FIZZ_JSON;

			assertThat(evaluate(json, "foo.bar", Integer.class)).isEqualTo(4);
			assertThat(evaluate(json, "foo.fizz", Integer.class)).isEqualTo(5);
		}

		@Test
		void jsonGetValueConversionAsJsonNode() {
			// use JsonNode conversion
			JsonNode node = evaluate(PROPERTY_NAMES_JSON, "property.^[name == 'value1']", JsonNode.class);
			assertThat(node).isEqualTo(mapper.readTree("{\"name\":\"value1\"}"));
		}

		@Test
		void jsonGetValueConversionAsObjectNode() {
			// use ObjectNode conversion
			ObjectNode node = evaluate(PROPERTY_NAMES_JSON, "property.^[name == 'value1']", ObjectNode.class);
			assertThat(node).isEqualTo(mapper.readTree("{\"name\":\"value1\"}"));
		}

		@Test
		void jsonGetValueConversionAsArrayNode() {
			// use ArrayNode conversion
			ArrayNode node = evaluate(PROPERTY_NAMES_JSON, "property", ArrayNode.class);
			assertThat(node).isEqualTo(mapper.readTree("[{\"name\":\"value1\"},{\"name\":\"value2\"}]"));
		}

		@Test
		void comparingArrayNode() {
			Boolean actual = evaluate(PROPERTIES_WITH_NAMES_JSON, "property1 eq property2", Boolean.class);
			assertThat(actual).isTrue();
		}

		@Test
		void comparingJsonNode() {
			Boolean actual = evaluate(PROPERTIES_WITH_NAMES_JSON, "property1[0] eq property2[0]", Boolean.class);
			assertThat(actual).isTrue();
		}

		@Test
		void unsupportedString() {
			String xml = "<what>?</what>";
			assertThatExceptionOfType(SpelEvaluationException.class)
					.isThrownBy(() -> evaluate(xml, "what", Object.class));
		}

		@Test
		void unsupportedJson() {
			String json = "\"literal\"";
			assertThat(evaluate(json, "foo", Object.class)).isNull();
		}

	}

	protected <T> T evaluate(Object rootObject, String expression, Class<T> expectedType) {
		return parser.parseExpression(expression).getValue(context, rootObject, expectedType);
	}

	private static final String FOO_BAR_JSON = """
			{
				"foo": "bar"
			}
			""";

	private static final String FOO_BAR_FIZZ_JSON = """
			{
				"foo": {
					"bar":  4,
					"fizz": 5
				}
			}
			""";

	private static final String FOO_BAR_ARRAY_FIZZ_JSON = """
			{
				"foo": {
					"bar": [
						{"fizz": 5, "buzz": 6},
						{"fizz": 7},
						{"fizz": 8}
					]
				}
			}
			""";

	private static final String PROPERTY_NAMES_JSON = """
			{
				"property" : [
					{"name": "value1"},
					{"name": "value2"}
				]
			}
			""";

	private static final String PROPERTIES_WITH_NAMES_JSON = """
			{
				"property1": [
					{"name": "value1"},
					{"name": "value2"}
				],
				"property2": [
					{"name": "value1"},
					{"name": "value2"}
				]
			}
			""";

}
