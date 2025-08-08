/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
