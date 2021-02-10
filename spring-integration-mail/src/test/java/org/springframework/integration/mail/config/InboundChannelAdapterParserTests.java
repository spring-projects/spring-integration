/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.mail.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import javax.mail.Authenticator;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.mail.AbstractMailReceiver;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.integration.mail.SearchTermStrategy;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 1.0.5
 */
@SpringJUnitConfig
@DirtiesContext
public class InboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired
	@Qualifier("autoChannel.adapter")
	private SourcePollingChannelAdapter autoChannelAdapter;

	//==================== INT-982 =====================
	@Test
	public void pop3ShouldDeleteTrue() {
		AbstractMailReceiver receiver = this.getReceiver("pop3ShouldDeleteTrue");
		assertThat(receiver.getClass()).isEqualTo(Pop3MailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Boolean value = (Boolean) receiverAccessor.getPropertyValue("shouldDeleteMessages");
		assertThat(value).isTrue();
		assertThat(receiverAccessor.getPropertyValue("embeddedPartsAsBytes")).isEqualTo(Boolean.FALSE);
		assertThat(receiverAccessor.getPropertyValue("autoCloseFolder")).isEqualTo(false);
		assertThat(receiverAccessor.getPropertyValue("headerMapper")).isNotNull();
	}

	@Test
	public void imapShouldMarkMessagesAsRead() {
		AbstractMailReceiver receiver = this.getReceiver("imapShouldMarkAsReadTrue");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Boolean value = (Boolean) receiverAccessor.getPropertyValue("shouldMarkMessagesAsRead");
		assertThat(value).isTrue();
		assertThat(receiverAccessor.getPropertyValue("embeddedPartsAsBytes")).isEqualTo(Boolean.TRUE);
		assertThat(receiverAccessor.getPropertyValue("headerMapper")).isNull();
	}

	@Test
	public void pop3ShouldDeleteFalse() {
		AbstractMailReceiver receiver = this.getReceiver("pop3ShouldDeleteFalse");
		assertThat(receiver.getClass()).isEqualTo(Pop3MailReceiver.class);
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertThat(value).isFalse();
	}

	@Test
	public void imapShouldDeleteTrue() {
		AbstractMailReceiver receiver = this.getReceiver("imapShouldDeleteTrue");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Boolean value = (Boolean) receiverAccessor.getPropertyValue("shouldDeleteMessages");
		assertThat(value).isTrue();
		assertThat(receiverAccessor.getPropertyValue("simpleContent")).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void imapShouldDeleteFalse() {
		AbstractMailReceiver receiver = this.getReceiver("imapShouldDeleteFalse");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Boolean value = (Boolean) receiverAccessor.getPropertyValue("shouldDeleteMessages");
		assertThat(value).isFalse();
		assertThat(receiverAccessor.getPropertyValue("simpleContent")).isEqualTo(Boolean.FALSE);
	}


	//==================== INT-1158 ====================
	@Test
	public void pop3ShouldDeleteTrueProperty() {
		AbstractMailReceiver receiver = this.getReceiver("pop3ShouldDeleteTrueProperty");
		assertThat(receiver.getClass()).isEqualTo(Pop3MailReceiver.class);
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertThat(value).isTrue();
	}

	@Test
	public void pop3ShouldDeleteFalseProperty() {
		AbstractMailReceiver receiver = this.getReceiver("pop3ShouldDeleteFalseProperty");
		assertThat(receiver.getClass()).isEqualTo(Pop3MailReceiver.class);
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertThat(value).isFalse();
	}

	@Test
	public void imapShouldDeleteTrueProperty() {
		AbstractMailReceiver receiver = this.getReceiver("imapShouldDeleteTrueProperty");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertThat(value).isTrue();
		assertThat(TestUtils.getPropertyValue(receiver, "evaluationContext.beanResolver")).isNotNull();
	}

	@Test
	public void imapShouldDeleteFalseProperty() {
		AbstractMailReceiver receiver = this.getReceiver("imapShouldDeleteFalseProperty");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		Boolean value = (Boolean) new DirectFieldAccessor(receiver).getPropertyValue("shouldDeleteMessages");
		assertThat(value).isFalse();
	}


	//==================== INT-1159 ====================
	@Test
	public void pop3WithAuthenticator() {
		AbstractMailReceiver receiver = this.getReceiver("pop3WithAuthenticator");
		assertThat(receiver.getClass()).isEqualTo(Pop3MailReceiver.class);
		Object authenticator = new DirectFieldAccessor(receiver).getPropertyValue("javaMailAuthenticator");
		assertThat(authenticator).isNotNull();
		assertThat(authenticator).isEqualTo(context.getBean("testAuthenticator"));
	}

	@Test
	public void imapWithAuthenticator() {
		AbstractMailReceiver receiver = this.getReceiver("imapWithAuthenticator");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		Object authenticator = new DirectFieldAccessor(receiver).getPropertyValue("javaMailAuthenticator");
		assertThat(authenticator).isNotNull();
		assertThat(authenticator).isEqualTo(context.getBean("testAuthenticator"));
	}

	@Test
	public void imapIdleWithAuthenticator() {
		AbstractMailReceiver receiver = this.getReceiver("imapIdleWithAuthenticator");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		Object authenticator = new DirectFieldAccessor(receiver).getPropertyValue("javaMailAuthenticator");
		assertThat(authenticator).isNotNull();
		assertThat(authenticator).isEqualTo(context.getBean("testAuthenticator"));
	}


	@SuppressWarnings("unused")
	private static class TestAuthenticator extends Authenticator {

	}


	//==================== INT-1160 ====================
	@Test
	public void pop3WithMaxFetchSize() {
		AbstractMailReceiver receiver = this.getReceiver("pop3WithMaxFetchSize");
		assertThat(receiver.getClass()).isEqualTo(Pop3MailReceiver.class);
		Object value = new DirectFieldAccessor(receiver).getPropertyValue("maxFetchSize");
		assertThat(value).isEqualTo(11);
	}

	@Test
	public void pop3WithMaxFetchSizeFallsBackToPollerMax() {
		AbstractMailReceiver receiver = this.getReceiver("pop3WithMaxFetchSizeFallsBackToPollerMax");
		assertThat(receiver.getClass()).isEqualTo(Pop3MailReceiver.class);
		Object value = new DirectFieldAccessor(receiver).getPropertyValue("maxFetchSize");
		assertThat(value).isEqualTo(99);
	}

	@Test
	public void imapWithMaxFetchSize() {
		AbstractMailReceiver receiver = this.getReceiver("imapWithMaxFetchSize");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		Object value = new DirectFieldAccessor(receiver).getPropertyValue("maxFetchSize");
		assertThat(value).isEqualTo(22);
	}

	@Test
	public void imapIdleWithMaxFetchSize() {
		AbstractMailReceiver receiver = this.getReceiver("imapIdleWithMaxFetchSize");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		Object value = new DirectFieldAccessor(receiver).getPropertyValue("maxFetchSize");
		assertThat(value).isEqualTo(33);
	}


	//==================== INT-1161 ====================
	@Test
	public void pop3WithSession() {
		AbstractMailReceiver receiver = this.getReceiver("pop3WithSession");
		assertThat(receiver.getClass()).isEqualTo(Pop3MailReceiver.class);
		Object session = new DirectFieldAccessor(receiver).getPropertyValue("session");
		assertThat(session).isNotNull();
		assertThat(session).isEqualTo(context.getBean("testSession"));
	}

	@Test
	public void imapWithSession() {
		AbstractMailReceiver receiver = this.getReceiver("imapWithSession");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		Object session = new DirectFieldAccessor(receiver).getPropertyValue("session");
		assertThat(session).isNotNull();
		assertThat(session).isEqualTo(context.getBean("testSession"));
	}

	@Test
	public void imapIdleWithSession() {
		AbstractMailReceiver receiver = this.getReceiver("imapIdleWithSession");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		Object session = new DirectFieldAccessor(receiver).getPropertyValue("session");
		assertThat(session).isNotNull();
		assertThat(session).isEqualTo(context.getBean("testSession"));
	}


	//==================== INT-1162 ====================
	@Test
	public void pop3WithoutStoreUri() {
		AbstractMailReceiver receiver = this.getReceiver("pop3WithoutStoreUri");
		assertThat(receiver.getClass()).isEqualTo(Pop3MailReceiver.class);
		Object url = new DirectFieldAccessor(receiver).getPropertyValue("url");
		assertThat(url).isNull();
	}

	@Test
	public void imapWithoutStoreUri() {
		AbstractMailReceiver receiver = this.getReceiver("imapWithoutStoreUri");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		Object url = new DirectFieldAccessor(receiver).getPropertyValue("url");
		assertThat(url).isNull();
	}

	@Test
	public void imapIdleWithoutStoreUri() {
		AbstractMailReceiver receiver = this.getReceiver("imapIdleWithoutStoreUri");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		Object url = new DirectFieldAccessor(receiver).getPropertyValue("url");
		assertThat(url).isNull();
	}


	//==================== INT-1163 ====================
	@Test
	public void inboundChannelAdapterRequiresShouldDeleteMessages() {
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(
						"InboundChannelAdapterParserTests-invalidContext.xml", getClass()))
				.withCauseInstanceOf(SAXParseException.class);
	}


	//==================== INT-2800 ====================
	@Test
	public void imapWithSearchTermStrategy() {
		AbstractMailReceiver receiver = this.getReceiver("imapWithSearch");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object sts = receiverAccessor.getPropertyValue("searchTermStrategy");
		assertThat(sts).isNotNull();
		assertThat(sts).isSameAs(context.getBean(SearchTermStrategy.class));
		assertThat(receiverAccessor.getPropertyValue("userFlag")).isEqualTo("flagged");
	}

	@Test
	public void pop3WithSearchTermStrategy() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(
						"InboundChannelAdapterParserTests-pop3Search-context.xml", getClass()))
				.withMessageContaining("searchTermStrategy is only allowed with imap");
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
		assertThat(TestUtils.getPropertyValue(autoChannelAdapter, "outputChannel")).isSameAs(autoChannel);
	}

}
