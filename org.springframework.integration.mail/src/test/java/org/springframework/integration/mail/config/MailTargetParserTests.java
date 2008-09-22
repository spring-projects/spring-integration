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

package org.springframework.integration.mail.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.mail.MailHeaderGenerator;
import org.springframework.integration.mail.MailTarget;
import org.springframework.integration.message.Message;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSender;

/**
 * @author Mark Fisher
 */
public class MailTargetParserTests {

	@Test
	public void testTargetWithMailSenderReference() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"mailTargetParserTests.xml", this.getClass());
		MailTarget target = (MailTarget) context.getBean("targetWithMailSenderReference");
		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(target);
		MailSender mailSender = (MailSender) fieldAccessor.getPropertyValue("mailSender");
		assertNotNull(mailSender);
	}

	@Test
	public void testTargetWithHostProperty() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"mailTargetParserTests.xml", this.getClass());
		MailTarget target = (MailTarget) context.getBean("targetWithHostProperty");
		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(target);
		MailSender mailSender = (MailSender) fieldAccessor.getPropertyValue("mailSender");
		assertNotNull(mailSender);
	}

	@Test
	public void testTargetWithHeaderGeneratorReference() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"mailTargetParserTests.xml", this.getClass());
		MailTarget target = (MailTarget) context.getBean("targetWithHeaderGeneratorReference");
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
