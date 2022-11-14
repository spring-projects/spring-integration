/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.config;

import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 *
 */
public class FileOutboundChannelAdapterParserWithErrorsTests {

	@Test
	public void testSettingDirectoryAndDirectoryExpression() {

		try {
			new ClassPathXmlApplicationContext("FileOutboundChannelAdapterParserWithErrorsTests-context.xml",
					getClass()).close();
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage()).isEqualTo("Configuration problem: Either directory or " +
					"directory-expression must be provided but not both\nOffending " +
					"resource: class path " +
					"resource [org/springframework/integration/file/config" +
					"/FileOutboundChannelAdapterParserWithErrorsTests-context.xml]");
			return;
		}

		fail("Expected a BeanDefinitionParsingException to be thrown");

	}

	@Test
	public void testNotSettingBothDirectoryAndDirectoryExpression() {

		try {
			new ClassPathXmlApplicationContext("FileOutboundChannelAdapterParserWithErrors2Tests-context.xml",
					getClass()).close();
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage()).isEqualTo("Configuration problem: directory or directory-expression " +
					"is required\nOffending resource: class path resource " +
					"[org/springframework/integration/file/config/FileOutboundChannelAdapterParserWithErrors2Tests-context.xml]");
			return;
		}

		fail("Expected a BeanDefinitionParsingException to be thrown");

	}

}
