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

package org.springframework.integration.mail.config;

import org.junit.Test;

import org.springframework.beans.factory.xml.XmlBeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 */
public class FailedMailConfigurationTests {
	/**
	 * validates that if 'should-delete-messages' is not set the context fails
	 */
	@Test(expected = XmlBeanDefinitionStoreException.class)
	public void testImapIdleWithNoDeleteMessageAttribute() {
		new ClassPathXmlApplicationContext("failed-imap-config.xml", this.getClass()).close();
	}
	/**
	 * validates that if 'should-delete-messages' is not set the context fails
	 */
	@Test(expected = XmlBeanDefinitionStoreException.class)
	public void testAdapterWithNoDeleteMessageAttribute() {
		new ClassPathXmlApplicationContext("failed-adapter-config.xml", this.getClass()).close();
	}
}
