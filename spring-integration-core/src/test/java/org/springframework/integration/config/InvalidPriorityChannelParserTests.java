/*
 * Copyright 2002-2016 the original author or authors.
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

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Dave Syer
 * @author Manuel Jordan
 * @since 4.3
 */
public class InvalidPriorityChannelParserTests {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testMessageStoreAndCapacityIllegal() throws Exception {
		this.exception.expect(BeanDefinitionParsingException.class);
		this.exception.expectMessage(Matchers.containsString("'capacity' attribute is not allowed"));
		new ClassPathXmlApplicationContext("InvalidPriorityChannelWithMessageStoreAndCapacityParserTests.xml",
				getClass()).close();
	}

	@Test
	public void testComparatorAndMessageStoreIllegal() throws Exception {
		this.exception.expect(BeanDefinitionParsingException.class);
		this.exception.expectMessage(Matchers.containsString("The 'message-store' attribute is not allowed"));
		new ClassPathXmlApplicationContext("InvalidPriorityChannelWithComparatorAndMessageStoreParserTests.xml",
				getClass()).close();
	}

}
