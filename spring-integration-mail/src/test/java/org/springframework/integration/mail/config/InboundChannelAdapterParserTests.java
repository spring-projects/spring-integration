/*
 * Copyright 2002-2012 the original author or authors.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.mail.Authenticator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.mail.AbstractMailReceiver;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXParseException;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 1.0.5
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class InboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired @Qualifier("autoChannel.adapter")
	private SourcePollingChannelAdapter autoChannelAdapter;

	//==================== INT-982 =====================

	@Test
	public void pop3ShouldDeleteTrue() {
		AbstractMailReceiver receiver = this.getReceiver("pop3ShouldDeleteTrue");
		assertEquals(Pop3MailReceiver.class, receiver.getClass());
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertTrue(value);
	}
	
	@Test
	public void imapShouldMarkMessagesAsRead() {
		AbstractMailReceiver receiver = this.getReceiver("imapShouldMarkAsReadTrue");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldMarkMessagesAsRead");
		assertTrue(value);
	}

	@Test
	public void pop3ShouldDeleteFalse() {
		AbstractMailReceiver receiver = this.getReceiver("pop3ShouldDeleteFalse");
		assertEquals(Pop3MailReceiver.class, receiver.getClass());
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertFalse(value);
	}

	@Test
	public void imapShouldDeleteTrue() {
		AbstractMailReceiver receiver = this.getReceiver("imapShouldDeleteTrue");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertTrue(value);
	}

	@Test
	public void imapShouldDeleteFalse() {
		AbstractMailReceiver receiver = this.getReceiver("imapShouldDeleteFalse");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertFalse(value);
	}


	//==================== INT-1158 ====================

	@Test
	public void pop3ShouldDeleteTrueProperty() {
		AbstractMailReceiver receiver = this.getReceiver("pop3ShouldDeleteTrueProperty");
		assertEquals(Pop3MailReceiver.class, receiver.getClass());
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertTrue(value);
	}

	@Test
	public void pop3ShouldDeleteFalseProperty() {
		AbstractMailReceiver receiver = this.getReceiver("pop3ShouldDeleteFalseProperty");
		assertEquals(Pop3MailReceiver.class, receiver.getClass());
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertFalse(value);
	}

	@Test
	public void imapShouldDeleteTrueProperty() {
		AbstractMailReceiver receiver = this.getReceiver("imapShouldDeleteTrueProperty");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertTrue(value);
	}

	@Test
	public void imapShouldDeleteFalseProperty() {
		AbstractMailReceiver receiver = this.getReceiver("imapShouldDeleteFalseProperty");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertFalse(value);
	}


	//==================== INT-1159 ====================

	@Test
	public void pop3WithAuthenticator() {
		AbstractMailReceiver receiver = this.getReceiver("pop3WithAuthenticator");
		assertEquals(Pop3MailReceiver.class, receiver.getClass());
		Object authenticator = new DirectFieldAccessor(receiver).getPropertyValue("javaMailAuthenticator");
		assertNotNull(authenticator);
		assertEquals(context.getBean("testAuthenticator"), authenticator);
	}

	@Test
	public void imapWithAuthenticator() {
		AbstractMailReceiver receiver = this.getReceiver("imapWithAuthenticator");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Object authenticator = new DirectFieldAccessor(receiver).getPropertyValue("javaMailAuthenticator");
		assertNotNull(authenticator);
		assertEquals(context.getBean("testAuthenticator"), authenticator);
	}

	@Test
	public void imapIdleWithAuthenticator() {
		AbstractMailReceiver receiver = this.getReceiver("imapIdleWithAuthenticator");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Object authenticator = new DirectFieldAccessor(receiver).getPropertyValue("javaMailAuthenticator");
		assertNotNull(authenticator);
		assertEquals(context.getBean("testAuthenticator"), authenticator);
	}


	@SuppressWarnings("unused")
	private static class TestAuthenticator extends Authenticator {
	}


	//==================== INT-1160 ====================

	@Test
	public void pop3WithMaxFetchSize() {
		AbstractMailReceiver receiver = this.getReceiver("pop3WithMaxFetchSize");
		assertEquals(Pop3MailReceiver.class, receiver.getClass());
		Object value = new DirectFieldAccessor(receiver).getPropertyValue("maxFetchSize");
		assertEquals(11, value);
	}

	@Test
	public void pop3WithMaxFetchSizeFallsBackToPollerMax() {
		AbstractMailReceiver receiver = this.getReceiver("pop3WithMaxFetchSizeFallsBackToPollerMax");
		assertEquals(Pop3MailReceiver.class, receiver.getClass());
		Object value = new DirectFieldAccessor(receiver).getPropertyValue("maxFetchSize");
		assertEquals(99, value);
	}

	@Test
	public void imapWithMaxFetchSize() {
		AbstractMailReceiver receiver = this.getReceiver("imapWithMaxFetchSize");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Object value = new DirectFieldAccessor(receiver).getPropertyValue("maxFetchSize");
		assertEquals(22, value);
	}

	@Test
	public void imapIdleWithMaxFetchSize() {
		AbstractMailReceiver receiver = this.getReceiver("imapIdleWithMaxFetchSize");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Object value = new DirectFieldAccessor(receiver).getPropertyValue("maxFetchSize");
		assertEquals(33, value);
	}


	//==================== INT-1161 ====================

	@Test
	public void pop3WithSession() {
		AbstractMailReceiver receiver = this.getReceiver("pop3WithSession");
		assertEquals(Pop3MailReceiver.class, receiver.getClass());
		Object session = new DirectFieldAccessor(receiver).getPropertyValue("session");
		assertNotNull(session);
		assertEquals(context.getBean("testSession"), session);
	}

	@Test
	public void imapWithSession() {
		AbstractMailReceiver receiver = this.getReceiver("imapWithSession");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Object session = new DirectFieldAccessor(receiver).getPropertyValue("session");
		assertNotNull(session);
		assertEquals(context.getBean("testSession"), session);
	}

	@Test
	public void imapIdleWithSession() {
		AbstractMailReceiver receiver = this.getReceiver("imapIdleWithSession");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Object session = new DirectFieldAccessor(receiver).getPropertyValue("session");
		assertNotNull(session);
		assertEquals(context.getBean("testSession"), session);
	}


	//==================== INT-1162 ====================

	@Test
	public void pop3WithoutStoreUri() {
		AbstractMailReceiver receiver = this.getReceiver("pop3WithoutStoreUri");
		assertEquals(Pop3MailReceiver.class, receiver.getClass());
		Object url = new DirectFieldAccessor(receiver).getPropertyValue("url");
		assertNull(url);
	}

	@Test
	public void imapWithoutStoreUri() {
		AbstractMailReceiver receiver = this.getReceiver("imapWithoutStoreUri");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Object url = new DirectFieldAccessor(receiver).getPropertyValue("url");
		assertNull(url);
	}

	@Test
	public void imapIdleWithoutStoreUri() {
		AbstractMailReceiver receiver = this.getReceiver("imapIdleWithoutStoreUri");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		Object url = new DirectFieldAccessor(receiver).getPropertyValue("url");
		assertNull(url);
	}


	//==================== INT-1163 ====================

	@Test
	public void inboundChannelAdapterRequiresShouldDeleteMessages() {
		try {
			new ClassPathXmlApplicationContext(
					"org/springframework/integration/mail/config/InboundChannelAdapterParserTests-invalidContext.xml");
			fail("expected a parser error");
		}
		catch(BeanDefinitionStoreException e) {
			assertEquals(SAXParseException.class, e.getCause().getClass());
		}
	}


	//===================== COMMON =====================

	private AbstractMailReceiver getReceiver(String name) {
		Object adapter = context.getBean(name);
		Object target = (adapter instanceof ImapIdleChannelAdapter) ? adapter
				: new DirectFieldAccessor(adapter).getPropertyValue("source");
		return (AbstractMailReceiver) new DirectFieldAccessor(target).getPropertyValue("mailReceiver");
	}

	@Test
	public void testAutoChannel() {
		assertSame(autoChannel, TestUtils.getPropertyValue(autoChannelAdapter, "outputChannel"));
	}

}
