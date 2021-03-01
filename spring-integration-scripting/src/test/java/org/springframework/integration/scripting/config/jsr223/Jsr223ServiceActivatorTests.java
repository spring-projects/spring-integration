/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.scripting.config.jsr223;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
public class Jsr223ServiceActivatorTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Autowired
	private MessageChannel inlineScriptInput;

	@Autowired
	private MessageChannel withScriptVariableGenerator;

	@Test
	public void referencedScript() throws Exception {

		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		for (int i = 1; i <= 3; i++) {
			Message<?> message = MessageBuilder.withPayload("test-" + i).setReplyChannel(replyChannel).build();
			this.referencedScriptInput.send(message);
			Thread.sleep(1000);
		}
		String value1 = (String) replyChannel.receive(0).getPayload();
		String value2 = (String) replyChannel.receive(0).getPayload();
		String value3 = (String) replyChannel.receive(0).getPayload();
		assertThat(value1.startsWith("python-test-1-foo (foo2) - bar")).isTrue();
		assertThat(value2.startsWith("python-test-2-foo (foo2) - bar")).isTrue();
		assertThat(value3.startsWith("python-test-3-foo (foo2) - bar")).isTrue();

		// because we are using 'prototype bean the suffix date will be
		// different
		assertThat(value1.substring(value1.indexOf(":") + 1, value1.lastIndexOf(":"))
				.equals(value2.substring(value1.indexOf(":") + 1, value1.lastIndexOf(":")))).isFalse();
		assertThat(value1.substring(value1.indexOf(":") + 1, value1.lastIndexOf(":"))
				.equals(value1.substring(value1.lastIndexOf(":")))).isFalse();

		assertThat(value2.substring(value1.indexOf(":") + 1, value1.lastIndexOf(":"))
				.equals(value3.substring(value1.indexOf(":") + 1, value1.lastIndexOf(":")))).isFalse();

		assertThat(value1.substring(value1.lastIndexOf(":") + 1)
				.equals(value2.substring(value1.lastIndexOf(":") + 1))).isFalse();
		assertThat(value2.substring(value1.lastIndexOf(":") + 1)
				.equals(value3.substring(value1.lastIndexOf(":") + 1))).isFalse();

		assertThat(replyChannel.receive(0)).isNull();
	}

	@Test
	public void withScriptVariableGenerator() throws Exception {

		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		for (int i = 1; i <= 3; i++) {
			Message<?> message = MessageBuilder.withPayload("test-" + i).setReplyChannel(replyChannel).build();
			this.withScriptVariableGenerator.send(message);
			Thread.sleep(1000);
		}
		String value1 = (String) replyChannel.receive(0).getPayload();
		String value2 = (String) replyChannel.receive(0).getPayload();
		String value3 = (String) replyChannel.receive(0).getPayload();
		assertThat(value1.startsWith("ruby-test-1-foo - bar")).isTrue();
		assertThat(value2.startsWith("ruby-test-2-foo - bar")).isTrue();
		assertThat(value3.startsWith("ruby-test-3-foo - bar")).isTrue();
		// because we are using 'prototype bean the suffix date will be
		// different

		assertThat(value1.substring(26).equals(value2.substring(26))).isFalse();
		assertThat(value2.substring(26).equals(value3.substring(26))).isFalse();

		assertThat(replyChannel.receive(0)).isNull();
	}

	@Test
	public void inlineScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		for (int i = 1; i <= 3; i++) {
			Message<?> message = MessageBuilder.withPayload("test-" + i).setReplyChannel(replyChannel).build();
			this.inlineScriptInput.send(message);
		}
		String payload = (String) replyChannel.receive(0).getPayload();

		assertThat(payload).startsWith("inline-test-1 - FOO");

		payload = (String) replyChannel.receive(0).getPayload();
		assertThat(payload).startsWith("inline-test-2 - FOO");

		payload = (String) replyChannel.receive(0).getPayload();
		assertThat(payload).startsWith("inline-test-3 - FOO");
		assertThat(payload.substring(payload.indexOf(":") + 1).matches(".+\\d{2}:\\d{2}:\\d{2}.+")).isTrue();

		assertThat(replyChannel.receive(0)).isNull();

	}

	@Test
	public void variablesAndScriptVariableGenerator() {
		assertThatExceptionOfType(BeansException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"Jsr223ServiceActivatorTests-fail-withgenerator-context.xml",
								getClass()))
				.withMessageContaining(
						"'script-variable-generator' and 'variable' sub-elements are mutually exclusive.");
	}

	@Test
	public void testDuplicateVariable() {
		assertThatExceptionOfType(BeansException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"Jsr223ServiceActivatorTests-fail-duplicated-variable-context.xml",
								getClass()))
				.withMessageContaining("Duplicated variable: foo");
	}

	public static class SampleScriptVariableSource implements ScriptVariableGenerator {

		@Override
		public Map<String, Object> generateScriptVariables(Message<?> message) {
			Map<String, Object> variables = new HashMap<>();
			variables.put("foo", "foo");
			variables.put("bar", "bar");
			variables.put("date", new Date());
			variables.put("payload", message.getPayload());
			variables.put("headers", message.getHeaders());
			return variables;
		}

	}

}
