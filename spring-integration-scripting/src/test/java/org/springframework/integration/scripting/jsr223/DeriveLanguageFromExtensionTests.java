/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting.jsr223;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
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
@EnabledForJreRange(min = JRE.JAVA_21, disabledReason = "JRuby 10.0.5.0")
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
