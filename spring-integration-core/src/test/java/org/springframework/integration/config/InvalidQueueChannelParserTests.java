/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Dave Syer
 * @author Manuel Jordan
 * @since 4.3
 */
public class InvalidQueueChannelParserTests {

	@Test
	public void testMessageStoreAndCapacityIllegal() {
		assertThatThrownBy(() ->
				new ClassPathXmlApplicationContext("InvalidQueueChannelWithMessageStoreAndCapacityParserTests.xml",
						getClass()))
				.isInstanceOf(BeanDefinitionParsingException.class)
				.hasMessageContaining("'capacity' attribute is not allowed");
	}

	@Test
	public void testRefAndCapacityIllegal() {
		assertThatThrownBy(() ->
				new ClassPathXmlApplicationContext("InvalidQueueChannelWithRefAndCapacityParserTests.xml",
						getClass()))
				.isInstanceOf(BeanDefinitionParsingException.class)
				.hasMessageContaining("'capacity' attribute is not allowed");
	}

	@Test
	public void testRefAndMessageStoreIllegal() {
		assertThatThrownBy(() ->
				new ClassPathXmlApplicationContext("InvalidQueueChannelWithRefAndMessageStoreParserTests.xml",
						getClass()))
				.isInstanceOf(BeanDefinitionParsingException.class)
				.hasMessageContaining("The 'message-store' attribute is not allowed " +
						"when providing a 'ref' to a custom queue.");
	}

}
