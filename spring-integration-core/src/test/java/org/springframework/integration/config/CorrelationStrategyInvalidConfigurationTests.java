/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Marius Bogoevici
 * @author Gary Russell
 * @author Artem Bilan
 */
public class CorrelationStrategyInvalidConfigurationTests {

	@Test
	public void testCorrelationStrategyWithVoidReturningMethods() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext("correlationStrategyWithVoidMethods.xml",
						getClass()))
				.withStackTraceContaining("MessageCountReleaseStrategy] has no eligible methods");
	}

	public static class VoidReturningCorrelationStrategy {

		public void invalidCorrelationMethod(String string) {
			//do nothing
		}

	}

}
