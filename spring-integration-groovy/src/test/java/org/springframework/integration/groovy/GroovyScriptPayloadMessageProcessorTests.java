/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.groovy;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import groovy.lang.Binding;
import groovy.lang.MissingPropertyException;
import org.junit.jupiter.api.Test;

import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.scripting.DefaultScriptVariableGenerator;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gunnar Hillert
 *
 * @since 2.0
 */
public class GroovyScriptPayloadMessageProcessorTests {

	private final AtomicInteger countHolder = new AtomicInteger();

	private GroovyCommandMessageProcessor processor;

	@Test
	public void testSimpleExecution() {
		int count = countHolder.getAndIncrement();
		Message<?> message = MessageBuilder.withPayload("headers.foo" + count).setHeader("foo" + count, "bar").build();
		processor = new GroovyCommandMessageProcessor();
		Object result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("bar");
	}

	@Test
	public void testDoubleExecutionWithNewScript() {
		processor = new GroovyCommandMessageProcessor();
		Message<?> message = MessageBuilder.withPayload("headers.foo").setHeader("foo", "bar").build();
		Object result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("bar");
		message = MessageBuilder.withPayload("headers.bar").setHeader("bar", "spam").build();
		result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("spam");
	}

	@Test
	public void testSimpleExecutionWithContext() {
		Message<?> message = MessageBuilder.withPayload("\"spam is $spam foo is $headers.foo\"")
				.setHeader("foo", "bar").build();
		ScriptVariableGenerator scriptVariableGenerator =
				new DefaultScriptVariableGenerator(Collections.singletonMap("spam", "bucket"));
		MessageProcessor<Object> processor = new GroovyCommandMessageProcessor(scriptVariableGenerator);
		Object result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("spam is bucket foo is bar");
	}

	@Test
	public void testBindingOverwrite() {
		Binding binding = new Binding() {

			@Override
			public Object getVariable(String name) {
				throw new RuntimeException("intentional");
			}
		};
		Message<?> message = MessageBuilder.withPayload("foo").build();
		processor = new GroovyCommandMessageProcessor(binding);
		assertThatExceptionOfType(RuntimeException.class)
				.isThrownBy(() -> processor.processMessage(message))
				.withMessage("intentional");
	}

	@Test
	public void testBindingOverwriteWithContext() {
		final String defaultValue = "default";
		Binding binding = new Binding() {

			@Override
			public Object getVariable(String name) {
				try {
					return super.getVariable(name);
				}
				catch (MissingPropertyException e) {
					// ignore
				}
				return defaultValue;
			}
		};
		ScriptVariableGenerator scriptVariableGenerator =
				new DefaultScriptVariableGenerator(Collections.singletonMap("spam", "bucket"));
		Message<?> message = MessageBuilder.withPayload("\"spam is $spam, foo is $foo\"").build();
		processor = new GroovyCommandMessageProcessor(binding, scriptVariableGenerator);
		Object result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("spam is bucket, foo is default");
	}

}
