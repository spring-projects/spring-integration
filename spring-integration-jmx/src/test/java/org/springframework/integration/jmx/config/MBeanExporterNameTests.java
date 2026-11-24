/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
