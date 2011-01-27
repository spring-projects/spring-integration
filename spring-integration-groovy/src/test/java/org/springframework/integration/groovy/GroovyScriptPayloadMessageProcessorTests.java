/*
 * Copyright 2002-2010 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.annotation.Repeat;

/**
 * @author Dave Syer
 * @since 2.0
 */
public class GroovyScriptPayloadMessageProcessorTests {

	@Rule
	public RepeatProcessor repeater = new RepeatProcessor(4);

	private AtomicInteger countHolder = new AtomicInteger();

	private GroovyCommandMessageProcessor processor = new GroovyCommandMessageProcessor();

	@Test
	@Repeat(20)
	public void testSimpleExecution() throws Exception {
		int count = countHolder.getAndIncrement();
		Message<?> message = MessageBuilder.withPayload("headers.foo" + count).setHeader("foo" + count, "bar").build();
		Object result = processor.processMessage(message);
		assertEquals("bar", result.toString());
	}

	@Test
	public void testDoubleExecutionWithNewScript() throws Exception {
		Message<?> message = MessageBuilder.withPayload("headers.foo").setHeader("foo", "bar").build();
		Object result = processor.processMessage(message);
		assertEquals("bar", result.toString());
		message = MessageBuilder.withPayload("headers.bar").setHeader("bar", "spam").build();
		result = processor.processMessage(message);
		assertEquals("spam", result.toString());
	}

	@Test
	public void testSimpleExecutionWithContext() throws Exception {
		Message<?> message = MessageBuilder.withPayload("\"spam is $spam foo is $headers.foo\"")
				.setHeader("foo", "bar").build();
		ScriptVariableSource scriptVariableSource = 
			new DefaultScriptVariableSource(Collections.singletonMap("spam",(Object)"bucket"));
		MessageProcessor<Object> processor = new GroovyCommandMessageProcessor(scriptVariableSource);
		Object result = processor.processMessage(message);
		assertEquals("spam is bucket foo is bar", result.toString());
	}

}
