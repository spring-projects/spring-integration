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

package org.springframework.integration.groovy.config;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import groovy.lang.GroovyObject;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GroovyControlBusTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;
	
	@Autowired
	private MyGroovyCustomizer groovyCustomizer;

	@Test
	public void testOperationOfControlBus() { // long is > 3
		this.groovyCustomizer.executed = false;
		Message<?> message = MessageBuilder.withPayload("def result = service.convert('aardvark'); def foo = headers.foo; result+foo").setHeader("foo", "bar").build();
		this.input.send(message);
		assertEquals("catbar", output.receive(0).getPayload());
		assertNull(output.receive(0));
		assertTrue(this.groovyCustomizer.executed);
	}

	@ManagedResource
	public static class Service {

		@ManagedOperation
		public String convert(String input) {
			return "cat";
		}
	}
	
	public static class MyGroovyCustomizer implements GroovyObjectCustomizer{
		private volatile boolean executed;
		
		public void customize(GroovyObject goo) {
			this.executed = true;
		}
		
	}

}
