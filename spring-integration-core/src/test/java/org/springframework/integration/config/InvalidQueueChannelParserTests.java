/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config;

import org.junit.jupiter.api.Test;

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
