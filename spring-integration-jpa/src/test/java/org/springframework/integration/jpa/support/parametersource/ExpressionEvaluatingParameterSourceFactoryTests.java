/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.jpa.support.parametersource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.jpa.support.JpaParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
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
		assertThat(source.hasValue("foo")).isTrue();
		assertThat(source.getValue("foo")).isEqualTo("bar");
	}

	@Test
	public void testMapInput() {
		ParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		assertThat(source.hasValue("foo")).isTrue();
		assertThat(source.getValue("foo")).isEqualTo("bar");
	}

	@Test
	public void testListOfMapsInput() {
		ParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "foo";
		assertThat(source.hasValue(expression)).isTrue();
		assertThat(source.getValue(expression).toString()).isEqualTo("[bar, bucket]");
	}

	@Test
	public void testMapInputWithExpression() {
		ParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		// This is an illegal parameter name in Spring JDBC so we'd never get this as input
		assertThat(source.hasValue("foo.toUpperCase()")).isTrue();
		assertThat(source.getValue("foo.toUpperCase()")).isEqualTo("BAR");
	}

	@Test
	public void testMapInputWithMappedExpression() {
		factory.setParameters(Collections.singletonList(new JpaParameter("spam", null, "foo.toUpperCase()")));
		ParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		assertThat(source.hasValue("spam")).isTrue();
		assertThat(source.getValue("spam")).isEqualTo("BAR");
	}

	@Test
	public void testMapInputWithMappedExpressionResolveStatic() {
		List<JpaParameter> parameters = new ArrayList<>();
		parameters.add(new JpaParameter("spam", null, "#staticParameters['foo'].toUpperCase()"));
		parameters.add(new JpaParameter("foo", "bar", null));
		factory.setParameters(parameters);

		ParameterSource source = factory.createParameterSource(Collections.singletonMap("crap", "bucket"));
		assertThat(source.hasValue("spam")).isTrue();
		assertThat(source.getValue("spam")).isEqualTo("BAR");
	}

	@Test
	public void testListOfMapsInputWithExpression() {
		factory.setParameters(Collections.singletonList(new JpaParameter("spam", null, "foo.toUpperCase()")));
		ParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "spam";
		assertThat(source.hasValue(expression)).isTrue();
		assertThat(source.getValue(expression).toString()).isEqualTo("[BAR, BUCKET]");
	}

	@Test
	public void testPositionalStaticParameters() {
		List<JpaParameter> parameters = new ArrayList<>();
		parameters.add(new JpaParameter("foo", null));
		parameters.add(new JpaParameter("bar", null));
		factory.setParameters(parameters);

		PositionSupportingParameterSource source = factory.createParameterSource("not important");

		String position0 = (String) source.getValueByPosition(1);
		String position1 = (String) source.getValueByPosition(2);

		assertThat(position0).isEqualTo("foo");
		assertThat(position1).isEqualTo("bar");
	}

	@Test
	public void testPositionalExpressionParameters() {
		List<JpaParameter> parameters = new ArrayList<>();
		parameters.add(new JpaParameter(null, "#root.toUpperCase()"));
		parameters.add(new JpaParameter("bar", null));
		factory.setParameters(parameters);

		PositionSupportingParameterSource source = factory.createParameterSource("very important");

		String position0 = (String) source.getValueByPosition(1);
		String position1 = (String) source.getValueByPosition(2);

		assertThat(position0).isEqualTo("VERY IMPORTANT");
		assertThat(position1).isEqualTo("bar");
	}

	@Test
	public void testPositionalExpressionParameters2() {
		List<JpaParameter> parameters = new ArrayList<>();

		parameters.add(new JpaParameter("bar", null));
		parameters.add(new JpaParameter(null, "#root.toUpperCase()"));

		factory.setParameters(parameters);

		PositionSupportingParameterSource source = factory.createParameterSource("very important");

		String position0 = (String) source.getValueByPosition(1);
		String position1 = (String) source.getValueByPosition(2);

		assertThat(position1).isEqualTo("VERY IMPORTANT");
		assertThat(position0).isEqualTo("bar");
	}

}
