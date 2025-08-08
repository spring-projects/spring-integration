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
 * @since 4.3
 */
public class InvalidQueueChannelParserTests {

	@Test
	public void testMessageStoreAndCapacityIllegal() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"InvalidQueueChannelWithMessageStoreAndCapacityParserTests.xml", getClass()))
				.withMessageContaining("'capacity' attribute is not allowed");
	}

	@Test
	public void testRefAndCapacityIllegal() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"InvalidQueueChannelWithRefAndCapacityParserTests.xml", getClass()))
				.withMessageContaining("'capacity' attribute is not allowed");
	}

	@Test
	public void testRefAndMessageStoreIllegal() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"InvalidQueueChannelWithRefAndMessageStoreParserTests.xml", getClass()))
				.withMessageContaining("The 'message-store' attribute is not allowed " +
						"when providing a 'ref' to a custom queue.");
	}

}
