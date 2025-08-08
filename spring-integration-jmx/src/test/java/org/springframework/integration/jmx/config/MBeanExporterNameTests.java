/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Dave Syer
 * @since 2.0
 */
public class MBeanExporterNameTests {

	@Test(expected = BeanDefinitionParsingException.class)
	public void testHandlerMBeanRegistration() throws Exception {
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass()).close();
	}

	public static class Source {

		public String get() {
			return "foo";
		}

	}

}
