/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.xml.XmlBeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class FailedMailConfigurationTests {

	/**
	 * Validates that if 'should-delete-messages' is not set the context fails
	 */
	@Test
	public void testImapIdleWithNoDeleteMessageAttribute() {
		assertThatExceptionOfType(XmlBeanDefinitionStoreException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext("failed-imap-config.xml", getClass()));
	}

	/**
	 * Validates that if 'should-delete-messages' is not set the context fails
	 */
	@Test
	public void testAdapterWithNoDeleteMessageAttribute() {
		assertThatExceptionOfType(XmlBeanDefinitionStoreException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext("failed-adapter-config.xml", getClass()));
	}

}
