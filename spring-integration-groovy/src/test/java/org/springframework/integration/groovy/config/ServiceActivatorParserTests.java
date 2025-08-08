/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy.config;

import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @since 2.2
 */
public class ServiceActivatorParserTests {

	@Test
	public void failExpressionAndScript() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail-expression-and-script-context.xml",
					this.getClass()).close();
			fail("Expected exception");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage()
					.startsWith("Configuration problem: Neither 'ref' nor 'expression' are permitted when " +
							"an inner script element is configured on element 'service-activator' with id='test'."))
					.isTrue();
		}
	}

}
