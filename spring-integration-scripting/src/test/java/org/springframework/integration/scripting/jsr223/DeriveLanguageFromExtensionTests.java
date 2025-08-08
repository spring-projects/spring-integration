/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting.jsr223;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author David Turanski
 * @author Artem Bilan
 *
 */
@SpringJUnitConfig
public class DeriveLanguageFromExtensionTests {

	@Autowired
	private ApplicationContext ctx;

	@Test
	public void testParseLanguage() {
		String[] langs = {"ruby", "Groovy", "python", "kotlin"};
		Class<?>[] executors = {
				RubyScriptExecutor.class,
				DefaultScriptExecutor.class,
				PythonScriptExecutor.class,
				DefaultScriptExecutor.class
		};

		Map<String, ScriptExecutingMessageProcessor> scriptProcessors =
				this.ctx.getBeansOfType(ScriptExecutingMessageProcessor.class);
		assertThat(scriptProcessors.size()).isEqualTo(4);

		for (int i = 0; i < 4; i++) {
			ScriptExecutingMessageProcessor processor = ctx.getBean(
					"org.springframework.integration.scripting.jsr223.ScriptExecutingMessageProcessor#" + i,
					ScriptExecutingMessageProcessor.class);

			AbstractScriptExecutor executor =
					TestUtils.getPropertyValue(processor, "scriptExecutor", AbstractScriptExecutor.class);
			assertThat(executor.getScriptEngine().getFactory().getLanguageName()).isEqualTo(langs[i]);
			assertThat(executor.getClass()).isEqualTo(executors[i]);
		}
	}

	@Test
	public void testBadExtension() {
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-fail1-context.xml",
								getClass()).close())
				.withStackTraceContaining("No suitable scripting engine found for extension 'xx'");
	}

	@Test
	public void testNoExtension() {
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-fail2-context.xml",
								getClass()).close())
				.withStackTraceContaining("Unable to determine language for script 'foo'");
	}

}
