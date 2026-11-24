/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
