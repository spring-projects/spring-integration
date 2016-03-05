/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.jpa.support.parametersource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.jpa.support.JpaParameter;

/**
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public class ExpressionEvaluatingParameterSourceFactoryTests {

	private final ExpressionEvaluatingParameterSourceFactory factory =
			new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));

	@Test
	public void testSetStaticParameters() {
		factory.setParameters(Collections.singletonList(new JpaParameter("foo", "bar", null)));
		ParameterSource source = factory.createParameterSource(null);
		assertTrue(source.hasValue("foo"));
		assertEquals("bar", source.getValue("foo"));
	}

	@Test
	public void testMapInput() {
		ParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		assertTrue(source.hasValue("foo"));
		assertEquals("bar", source.getValue("foo"));
	}

	@Test
	public void testListOfMapsInput() {
		@SuppressWarnings("unchecked")
		ParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "foo";
		assertTrue(source.hasValue(expression));
		assertEquals("[bar, bucket]", source.getValue(expression).toString());
	}

	@Test
	public void testMapInputWithExpression() {
		ParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		// This is an illegal parameter name in Spring JDBC so we'd never get this as input
		assertTrue(source.hasValue("foo.toUpperCase()"));
		assertEquals("BAR", source.getValue("foo.toUpperCase()"));
	}

	@Test
	public void testMapInputWithMappedExpression() {
		factory.setParameters(Collections.singletonList(new JpaParameter("spam", null, "foo.toUpperCase()")));
		ParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		assertTrue(source.hasValue("spam"));
		assertEquals("BAR", source.getValue("spam"));
	}

	@Test
	public void testMapInputWithMappedExpressionResolveStatic() {

		List<JpaParameter> parameters = new ArrayList<JpaParameter>();
		parameters.add(new JpaParameter("spam", null, "#staticParameters['foo'].toUpperCase()"));
		parameters.add(new JpaParameter("foo", "bar", null));
		factory.setParameters(parameters);

		ParameterSource source = factory.createParameterSource(Collections.singletonMap("crap", "bucket"));
		assertTrue(source.hasValue("spam"));
		assertEquals("BAR", source.getValue("spam"));
	}

	@Test
	public void testListOfMapsInputWithExpression() {
		factory.setParameters(Collections.singletonList(new JpaParameter("spam", null, "foo.toUpperCase()")));
		@SuppressWarnings("unchecked")
		ParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "spam";
		assertTrue(source.hasValue(expression));
		assertEquals("[BAR, BUCKET]", source.getValue(expression).toString());
	}

	@Test
	public void testPositionalStaticParameters() {
		List<JpaParameter> parameters = new ArrayList<JpaParameter>();
		parameters.add(new JpaParameter("foo", null));
		parameters.add(new JpaParameter("bar", null));
		factory.setParameters(parameters);

		PositionSupportingParameterSource source = factory.createParameterSource("not important");

		String position0 = (String) source.getValueByPosition(0);
		String position1 = (String) source.getValueByPosition(1);

		assertEquals("foo", position0);
		assertEquals("bar", position1);
	}

	@Test
	public void testPositionalExpressionParameters() {
		List<JpaParameter> parameters = new ArrayList<JpaParameter>();
		parameters.add(new JpaParameter(null, "#root.toUpperCase()"));
		parameters.add(new JpaParameter("bar", null));
		factory.setParameters(parameters);

		PositionSupportingParameterSource source = factory.createParameterSource("very important");

		String position0 = (String) source.getValueByPosition(0);
		String position1 = (String) source.getValueByPosition(1);

		assertEquals("VERY IMPORTANT", position0);
		assertEquals("bar", position1);
	}

	@Test
	public void testPositionalExpressionParameters2() {
		List<JpaParameter> parameters = new ArrayList<JpaParameter>();

		parameters.add(new JpaParameter("bar", null));
		parameters.add(new JpaParameter(null, "#root.toUpperCase()"));

		factory.setParameters(parameters);

		PositionSupportingParameterSource source = factory.createParameterSource("very important");

		String position0 = (String) source.getValueByPosition(0);
		String position1 = (String) source.getValueByPosition(1);

		assertEquals("VERY IMPORTANT", position1);
		assertEquals("bar", position0);
	}

}
