/*
 * Copyright 2002-2017 the original author or authors.
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
import static org.mockito.Mockito.mock;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.JdbcUtils;

/**
 * @author Dave Syer
 * @author Meherzad Lahewala
 */
public class ExpressionEvaluatingSqlParameterSourceFactoryTests {

	private final ExpressionEvaluatingSqlParameterSourceFactory factory =
			new ExpressionEvaluatingSqlParameterSourceFactory();

	@Test
	public void testSetStaticParameters() throws Exception {
		factory.setStaticParameters(Collections.singletonMap("foo", "bar"));
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(null);
		assertTrue(source.hasValue("foo"));
		assertEquals("bar", source.getValue("foo"));
		assertEquals(JdbcUtils.TYPE_UNKNOWN, source.getSqlType("foo"));
	}

	@Test
	public void testMapInput() throws Exception {
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		assertTrue(source.hasValue("foo"));
		assertEquals("bar", source.getValue("foo"));
		assertEquals(JdbcUtils.TYPE_UNKNOWN, source.getSqlType("foo"));
	}

	@Test
	public void testListOfMapsInput() throws Exception {
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "foo";
		assertTrue(source.hasValue(expression));
		assertEquals("[bar, bucket]", source.getValue(expression).toString());
		assertEquals(JdbcUtils.TYPE_UNKNOWN, source.getSqlType(expression));
	}

	@Test
	public void testMapInputWithExpression() throws Exception {
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		// This is an illegal parameter name in Spring JDBC so we'd never get this as input
		assertTrue(source.hasValue("foo.toUpperCase()"));
		assertEquals("BAR", source.getValue("foo.toUpperCase()"));
		assertEquals(JdbcUtils.TYPE_UNKNOWN, source.getSqlType("food"));
	}

	@Test
	public void testMapInputWithMappedExpression() throws Exception {
		factory.setParameterExpressions(Collections.singletonMap("spam", "foo.toUpperCase()"));
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		assertTrue(source.hasValue("spam"));
		assertEquals("BAR", source.getValue("spam"));
		assertEquals(JdbcUtils.TYPE_UNKNOWN, source.getSqlType("spam"));
	}

	@Test
	public void testMapInputWithMappedExpressionResolveStatic() throws Exception {
		factory.setParameterExpressions(Collections.singletonMap("spam", "#staticParameters['foo'].toUpperCase()"));
		factory.setStaticParameters(Collections.singletonMap("foo", "bar"));
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Collections.singletonMap("crap", "bucket"));
		assertTrue(source.hasValue("spam"));
		assertEquals("BAR", source.getValue("spam"));
		assertEquals(JdbcUtils.TYPE_UNKNOWN, source.getSqlType("spam"));
	}

	@Test
	public void testListOfMapsInputWithExpression() throws Exception {
		factory.setParameterExpressions(Collections.singletonMap("spam", "foo.toUpperCase()"));
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "spam";
		assertTrue(source.hasValue(expression));
		assertEquals("[BAR, BUCKET]", source.getValue(expression).toString());
		assertEquals(JdbcUtils.TYPE_UNKNOWN, source.getSqlType("foo"));
	}

	@Test
	public void testListOfMapsInputWithExpressionAndTypes() throws Exception {
		factory.setParameterExpressions(Collections.singletonMap("spam", "foo.toUpperCase()"));
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.setSqlParameterTypes(Collections.singletonMap("spam", Types.SQLXML));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "spam";
		assertTrue(source.hasValue(expression));
		assertEquals("[BAR, BUCKET]", source.getValue(expression).toString());
		assertEquals(Types.SQLXML, source.getSqlType("spam"));
	}

	@Test
	public void testListOfMapsInputWithExpressionAndEmptyTypes() throws Exception {
		factory.setParameterExpressions(Collections.singletonMap("spam", "foo.toUpperCase()"));
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.setSqlParameterTypes(Collections.emptyMap());
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "spam";
		assertTrue(source.hasValue(expression));
		assertEquals("[BAR, BUCKET]", source.getValue(expression).toString());
		assertEquals(JdbcUtils.TYPE_UNKNOWN, source.getSqlType("spam"));
	}

}
