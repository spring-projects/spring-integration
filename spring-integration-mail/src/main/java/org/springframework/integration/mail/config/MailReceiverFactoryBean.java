/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Locale;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Session;
import jakarta.mail.URLName;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.expression.Expression;
import org.springframework.integration.mail.AbstractMailReceiver;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.MailReceiver;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.integration.mail.SearchTermStrategy;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 1.0.3
 */
public class MailReceiverFactoryBean extends AbstractFactoryBean<MailReceiver> {

	private String storeUri;

	private String protocol;

	private Session session;

	private MailReceiver receiver;

	private Properties javaMailProperties;

	private Authenticator authenticator;

	/**
	 * Indicates whether retrieved messages should be deleted from the server.
	 * This value will be <code>null</code> <i>unless</i> explicitly configured.
	 */
	private Boolean shouldDeleteMessages = null;

	private Boolean shouldMarkMessagesAsRead = null;

	private int maxFetchSize = 1;

	private Expression selectorExpression;

	private SearchTermStrategy searchTermStrategy;

	private String userFlag;

	private HeaderMapper<MimeMessage> headerMapper;

	private Boolean embeddedPartsAsBytes;

	private Boolean simpleContent;

	private Boolean autoCloseFolder;

	public void setStoreUri(@Nullable String storeUri) {
		this.storeUri = storeUri;
	}

	public void setProtocol(@Nullable String protocol) {
		this.protocol = protocol;
	}

	public void setSession(@Nullable Session session) {
		this.session = session;
	}

	public void setJavaMailProperties(@Nullable Properties javaMailProperties) {
		this.javaMailProperties = javaMailProperties;
	}

	public void setAuthenticator(@Nullable Authenticator authenticator) {
		this.authenticator = authenticator;
	}

	public void setShouldDeleteMessages(@Nullable Boolean shouldDeleteMessages) {
		this.shouldDeleteMessages = shouldDeleteMessages;
	}

	public void setShouldMarkMessagesAsRead(@Nullable Boolean shouldMarkMessagesAsRead) {
		this.shouldMarkMessagesAsRead = shouldMarkMessagesAsRead;
	}

	public Boolean isShouldMarkMessagesAsRead() {
		return this.shouldMarkMessagesAsRead != null && this.shouldMarkMessagesAsRead;
	}

	public void setMaxFetchSize(int maxFetchSize) {
		this.maxFetchSize = maxFetchSize;
	}

	public void setSelectorExpression(@Nullable Expression selectorExpression) {
		this.selectorExpression = selectorExpression;
	}

	public void setSearchTermStrategy(@Nullable SearchTermStrategy searchTermStrategy) {
		this.searchTermStrategy = searchTermStrategy;
	}

	public void setUserFlag(@Nullable String userFlag) {
		this.userFlag = userFlag;
	}

	public void setHeaderMapper(@Nullable HeaderMapper<MimeMessage> headerMapper) {
		this.headerMapper = headerMapper;
	}

	public void setEmbeddedPartsAsBytes(@Nullable Boolean embeddedPartsAsBytes) {
		this.embeddedPartsAsBytes = embeddedPartsAsBytes;
	}

	public void setSimpleContent(@Nullable Boolean simpleContent) {
		this.simpleContent = simpleContent;
	}

	public void setAutoCloseFolder(@Nullable Boolean autoCloseFolder) {
		this.autoCloseFolder = autoCloseFolder;
	}

	@Override
	protected MailReceiver createInstance() {
		if (this.receiver == null) {
			this.receiver = this.createReceiver();
		}
		return this.receiver;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.receiver != null) ? this.receiver.getClass() : MailReceiver.class;
	}

	private MailReceiver createReceiver() { // NOSONAR
		verifyProtocol();
		boolean isPop3 = this.protocol.toLowerCase(Locale.ROOT).startsWith("pop3");
		boolean isImap = this.protocol.toLowerCase(Locale.ROOT).startsWith("imap");
		Assert.isTrue(isPop3 || isImap, "the store URI must begin with 'pop3' or 'imap'");
		AbstractMailReceiver mailReceiver = isPop3
				? new Pop3MailReceiver(this.storeUri)
				: new ImapMailReceiver(this.storeUri);
		if (this.session != null) {
			Assert.isNull(this.javaMailProperties,
					"JavaMail Properties are not allowed when a Session has been provided.");
			Assert.isNull(this.authenticator,
					"A JavaMail Authenticator is not allowed when a Session has been provided.");
			mailReceiver.setSession(this.session);
		}
		if (this.searchTermStrategy != null) {
			Assert.isTrue(isImap, "searchTermStrategy is only allowed with imap");
			((ImapMailReceiver) mailReceiver).setSearchTermStrategy(this.searchTermStrategy);
		}
		if (this.javaMailProperties != null) {
			mailReceiver.setJavaMailProperties(this.javaMailProperties);
		}
		if (this.authenticator != null) {
			mailReceiver.setJavaMailAuthenticator(this.authenticator);
		}
		if (this.shouldDeleteMessages != null) {
			// always set the value if configured explicitly
			// otherwise, the default is true for POP3 but false for IMAP
			mailReceiver.setShouldDeleteMessages(this.shouldDeleteMessages);
		}
		mailReceiver.setMaxFetchSize(this.maxFetchSize);
		mailReceiver.setSelectorExpression(this.selectorExpression);
		if (StringUtils.hasText(this.userFlag)) {
			mailReceiver.setUserFlag(this.userFlag);
		}

		if (isPop3) {
			if (isShouldMarkMessagesAsRead()) {
				this.logger.warn("Setting 'should-mark-messages-as-read' to 'true' while using POP3 has no effect");
			}
		}
		else {
			((ImapMailReceiver) mailReceiver).setShouldMarkMessagesAsRead(this.shouldMarkMessagesAsRead);
		}
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			mailReceiver.setBeanFactory(beanFactory);
		}
		if (this.headerMapper != null) {
			mailReceiver.setHeaderMapper(this.headerMapper);
		}
		if (this.embeddedPartsAsBytes != null) {
			mailReceiver.setEmbeddedPartsAsBytes(this.embeddedPartsAsBytes);
		}
		if (this.simpleContent != null) {
			mailReceiver.setSimpleContent(this.simpleContent);
		}
		if (this.autoCloseFolder != null) {
			mailReceiver.setAutoCloseFolder(this.autoCloseFolder);
		}
		mailReceiver.afterPropertiesSet();
		return mailReceiver;
	}

	private void verifyProtocol() {
		if (StringUtils.hasText(this.storeUri)) {
			URLName urlName = new URLName(this.storeUri);
			if (this.protocol == null) {
				this.protocol = urlName.getProtocol();
			}
			else {
				Assert.isTrue(this.protocol.equals(urlName.getProtocol()),
						"The provided 'protocol' does not match that in the 'storeUri'.");
			}
		}
		else {
			Assert.hasText(this.protocol, "Either the 'storeUri' or 'protocol' is required.");
		}
		Assert.hasText(this.protocol, "Unable to resolve protocol.");
	}

	@Override
	public void destroy() {
		if (this.receiver != null && this.receiver instanceof DisposableBean) {
			try {
				((DisposableBean) this.receiver).destroy();
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

}
