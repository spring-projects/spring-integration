/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting.jsr223;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 * @author Artem Bilan
 *
 */
public class Jsr223ScriptExecutingMessageProcessorTests {

	private static final ScriptExecutor SCRIPT_EXECUTOR = ScriptExecutorFactory.getScriptExecutor("jruby");

	@Test
	public void testExecuteWithVariables() {
		Map<String, Object> vars = new HashMap<>();
		vars.put("one", 1);
		vars.put("two", "two");
		vars.put("three", 3);

		ScriptSource scriptSource =
				new ResourceScriptSource(
						new ClassPathResource("/org/springframework/integration/scripting/jsr223/print_message.rb"));

		ScriptExecutingMessageProcessor messageProcessor =
				new ScriptExecutingMessageProcessor(scriptSource, SCRIPT_EXECUTOR, vars);
		messageProcessor.setBeanFactory(Mockito.mock(BeanFactory.class));

		Message<?> message = new GenericMessage<>("hello");

		Object obj = messageProcessor.processMessage(message);

		assertThat(obj.toString()).contains("hello modified 1 two 3");
	}

	@Test
	public void testWithNoVars() {
		ScriptSource scriptSource =
				new ResourceScriptSource(
						new ClassPathResource("/org/springframework/integration/scripting/jsr223/print_message.rb"));

		ScriptExecutingMessageProcessor messageProcessor =
				new ScriptExecutingMessageProcessor(scriptSource, SCRIPT_EXECUTOR);
		messageProcessor.setBeanFactory(Mockito.mock(BeanFactory.class));

		Message<?> message = new GenericMessage<>("hello");

		Object obj = messageProcessor.processMessage(message);

		assertThat(obj.toString()).contains("hello modified");
	}

}
