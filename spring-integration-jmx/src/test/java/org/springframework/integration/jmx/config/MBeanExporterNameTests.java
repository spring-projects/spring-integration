/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Dave Syer
 * @since 2.0
 */
public class MBeanExporterNameTests {

	@Test
	public void testHandlerMBeanRegistration() {
		Assertions.assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass()));
	}

	public static class Source {

		public String get() {
			return "foo";
		}

	}

}
