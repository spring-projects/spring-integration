/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.groovy;

import groovy.lang.Binding;
import groovy.lang.MissingPropertyException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * @author Artem Bilan
 * @since 2.2
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GroovyVariableResolverBindingTests {

	@Autowired
	private VariableResolver groovyVariableResolver;

	@Autowired
	private BeanFactory beanFactoryDecorator;

	@Autowired
	@Qualifier("channel")
	private MessageChannel channel;

	@Test
	public void testCustomVariableResolver() {
		Binding binding = new GroovyVariableResolverBinding(new TestVariableResolver());
		binding.setVariable("foo", "bar");
		Object bar = binding.getVariable("foo");
		assertEquals("bar", bar);
		Object testObject = binding.getVariable("testObject");
		assertEquals("testObject", testObject);
	}

	@Test(expected = MissingPropertyException.class)
	public void testMissingProperty() {
		Binding binding = new GroovyVariableResolverBinding(new TestVariableResolver());
		try {
			binding.getVariable("foo");
			fail("Expected MissingPropertyException");
		}
		catch (Exception e) {
			assertTrue("Expected MissingPropertyException, got " + e.getClass() + ":" + e.getMessage(), e instanceof MissingPropertyException);
			assertTrue(e.getMessage().contains("foo"));
			throw (MissingPropertyException) e;
		}
	}

	@Test
	public void testOriginalBeanFactory() {
		Binding binding = new GroovyVariableResolverBinding(this.groovyVariableResolver);
		Object channel = binding.getVariable("channel");
		assertSame(this.channel, channel);
	}

	@Test(expected = BeanCreationException.class)
	public void tesNotAllowedSpringBeanFromOriginalBeanFactory() {
		Binding binding = new GroovyVariableResolverBinding(this.groovyVariableResolver);
		try {
			MessageChannel scopedChannel = (MessageChannel) binding.getVariable("scopedChannel");
			scopedChannel.send(new GenericMessage<String>("test"));
			fail("Expected BeanCreationException");
		}
		catch (Exception e) {
			assertTrue("Expected BeanCreationException, got " + e.getClass() + ":" + e.getMessage(), e instanceof BeanCreationException);
			assertTrue(e.getMessage().contains("scopedChannel"));
			assertTrue(e.getMessage().contains("Scope 'request' is not active"));
			throw (BeanCreationException) e;
		}
	}

	@Test
	public void testFilteredBeanFactoryDecorator() {
		VariableResolver resolver = new BeanFactoryGroovyVariableResolver(this.beanFactoryDecorator);
		Binding binding = new GroovyVariableResolverBinding(resolver);
		Object channel = binding.getVariable("channel");
		assertSame(this.channel, channel);
	}

	public static class TestVariableResolver implements VariableResolver {

		Map<String, Object> variables = new HashMap<String, Object>();

		public TestVariableResolver() {
			this.variables.put("testObject", "testObject");
		}

		public Object resolveVariableName(String name) {
			return this.variables.get(name);
		}
	}

	public static class TestBeanFilter extends BeanFilterAdapter {

	}
}
