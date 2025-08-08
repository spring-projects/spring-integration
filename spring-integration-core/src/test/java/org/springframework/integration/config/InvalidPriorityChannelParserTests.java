/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 * @author Manuel Jordan
 * @author Artem Bilan
 *
 * @since 4.3
 */
public class InvalidPriorityChannelParserTests {

	@Test
	public void testMessageStoreAndCapacityIllegal() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"InvalidPriorityChannelWithMessageStoreAndCapacityParserTests.xml", getClass()))
				.withMessageContaining("'capacity' attribute is not allowed");
	}

	@Test
	public void testComparatorAndMessageStoreIllegal() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"InvalidPriorityChannelWithComparatorAndMessageStoreParserTests.xml", getClass()))
				.withMessageContaining("The 'message-store' attribute is not allowed");
	}

}
