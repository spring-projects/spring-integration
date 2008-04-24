/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.mail.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.mail.MailHeaderGenerator;
import org.springframework.integration.adapter.mail.MailTarget;
import org.springframework.integration.endpoint.TargetEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.Target;
import org.springframework.mail.MailMessage;

/**
 * @author Mark Fisher
 */
public class MailTargetAdapterParserTests {

	@Test
	public void testAdapterWithMailSenderReference() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"mailTargetAdapterParserTests.xml", this.getClass());
		TargetEndpoint endpoint = (TargetEndpoint) context.getBean("adapterWithMailSenderReference");
		Target target = endpoint.getTarget();
		assertNotNull(target);
		assertTrue(target instanceof MailTarget);
	}

	@Test
	public void testAdapterWithHostProperty() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"mailTargetAdapterParserTests.xml", this.getClass());
		TargetEndpoint endpoint = (TargetEndpoint) context.getBean("adapterWithHostProperty");
		Target target = endpoint.getTarget();
		assertNotNull(target);
		assertTrue(target instanceof MailTarget);
	}

	@Test
	public void testAdapterWithHeaderGeneratorReference() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"mailTargetAdapterParserTests.xml", this.getClass());
		TargetEndpoint endpoint = (TargetEndpoint) context.getBean("adapterWithHeaderGeneratorReference");
		Target target = endpoint.getTarget();
		assertNotNull(target);
		assertTrue(target instanceof MailTarget);
		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(target);
		MailHeaderGenerator headerGenerator =
				(MailHeaderGenerator) fieldAccessor.getPropertyValue("mailHeaderGenerator");
		assertEquals(TestHeaderGenerator.class, headerGenerator.getClass());
	}


	public static class TestHeaderGenerator implements MailHeaderGenerator {

		public void populateMailMessageHeader(MailMessage mailMessage, Message<?> message) {
		}
	}

}
