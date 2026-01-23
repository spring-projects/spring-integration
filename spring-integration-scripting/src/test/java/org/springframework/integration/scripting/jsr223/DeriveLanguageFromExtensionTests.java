/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.scripting.jsr223;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.scripting.PolyglotScriptExecutor;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author David Turanski
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class DeriveLanguageFromExtensionTests {

	@Autowired
	private ApplicationContext ctx;

	@ParameterizedTest
	@MethodSource("languageExecutorSource")
	public void testParseLanguage(String language, Class<?> executorClass, ArgumentsAccessor argumentsAccessor) {
		assertThat(this.ctx.getBeansOfType(ScriptExecutingMessageProcessor.class)).hasSize(5);

		var processor =
				ctx.getBean(
						"org.springframework.integration.scripting.jsr223.ScriptExecutingMessageProcessor#" +
								(argumentsAccessor.getInvocationIndex() - 1),
						ScriptExecutingMessageProcessor.class);

		ScriptExecutor executor = TestUtils.getPropertyValue(processor, "scriptExecutor");
		if (executor instanceof PolyglotScriptExecutor) {
			assertThat(TestUtils.<Object>getPropertyValue(executor, "language")).isEqualTo(language);
		}
		else {
			AbstractScriptExecutor abstractScriptExecutor = (AbstractScriptExecutor) executor;
			assertThat(abstractScriptExecutor.getScriptEngine().getFactory().getLanguageName()).isEqualTo(language);
		}
		assertThat(executor.getClass()).isEqualTo(executorClass);
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

	private static Stream<Arguments> languageExecutorSource() {
		return Stream.of(
				Arguments.of("ruby", RubyScriptExecutor.class),
				Arguments.of("Groovy", DefaultScriptExecutor.class),
				Arguments.of("python", PolyglotScriptExecutor.class),
				Arguments.of("kotlin", DefaultScriptExecutor.class),
				Arguments.of("js", PolyglotScriptExecutor.class));
	}

}
