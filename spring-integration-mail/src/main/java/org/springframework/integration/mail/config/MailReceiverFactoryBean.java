/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Session;
import javax.mail.URLName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.expression.Expression;
import org.springframework.integration.mail.AbstractMailReceiver;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.MailReceiver;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 1.0.3
 */
public class MailReceiverFactoryBean implements FactoryBean<MailReceiver>, DisposableBean {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile String storeUri;

	private volatile String protocol;

	private volatile Session session;

	private volatile MailReceiver receiver;

	private volatile Properties javaMailProperties;

	private volatile Authenticator authenticator;

	private volatile boolean synchronizedTx;

	/**
	 * Indicates whether retrieved messages should be deleted from the server.
	 * This value will be <code>null</code> <i>unless</i> explicitly configured.
	 */
	private volatile Boolean shouldDeleteMessages = null;

	private volatile Boolean shouldMarkMessagesAsRead = null;

	private volatile int maxFetchSize = 1;

	private volatile Expression selectorExpression;


	public void setStoreUri(String storeUri) {
		this.storeUri = storeUri;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void setJavaMailProperties(Properties javaMailProperties) {
		this.javaMailProperties = javaMailProperties;
	}

	public void setAuthenticator(Authenticator authenticator) {
		this.authenticator = authenticator;
	}

	public void setShouldDeleteMessages(Boolean shouldDeleteMessages) {
		this.shouldDeleteMessages = shouldDeleteMessages;
	}

	public void setShouldMarkMessagesAsRead(Boolean shouldMarkMessagesAsRead) {
		this.shouldMarkMessagesAsRead = shouldMarkMessagesAsRead;
	}

	public Boolean isShouldMarkMessagesAsRead() {
		return shouldMarkMessagesAsRead != null && shouldMarkMessagesAsRead;
	}

	public void setMaxFetchSize(int maxFetchSize) {
		this.maxFetchSize = maxFetchSize;
	}

	public void setSelectorExpression(Expression selectorExpression) {
		this.selectorExpression = selectorExpression;
	}

	public void setSynchronized(boolean synchronizedTx) {
		this.synchronizedTx = synchronizedTx;
	}

	public MailReceiver getObject() throws Exception {
		if (this.receiver == null) {
			this.receiver = this.createReceiver();
		}
		return this.receiver;
	}

	public Class<?> getObjectType() {
		return (this.receiver != null) ? this.receiver.getClass() : MailReceiver.class;
	}

	public boolean isSingleton() {
		return true;
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

	private MailReceiver createReceiver() {
		this.verifyProtocol();
		boolean isPop3 = this.protocol.toLowerCase().startsWith("pop3");
		boolean isImap = this.protocol.toLowerCase().startsWith("imap");
		Assert.isTrue(isPop3 || isImap, "the store URI must begin with 'pop3' or 'imap'");
		AbstractMailReceiver receiver = isPop3 ? new Pop3MailReceiver(this.storeUri) : new ImapMailReceiver(this.storeUri);
		if (this.session != null) {
			Assert.isNull(this.javaMailProperties, "JavaMail Properties are not allowed when a Session has been provided.");
			Assert.isNull(this.authenticator, "A JavaMail Authenticator is not allowed when a Session has been provied.");
			receiver.setSession(this.session);
		}
		if (this.javaMailProperties != null) {
			receiver.setJavaMailProperties(this.javaMailProperties);
		}
		if (this.authenticator != null) {
			receiver.setJavaMailAuthenticator(this.authenticator);
		}
		if (this.shouldDeleteMessages != null) {
			// always set the value if configured explicitly
			// otherwise, the default is true for POP3 but false for IMAP
			receiver.setShouldDeleteMessages(this.shouldDeleteMessages);
		}
		if (this.synchronizedTx && this.maxFetchSize != 1) {
			if (logger.isWarnEnabled()) {
				logger.warn("Max Fetch Size is set to 1 because the poller is synchronized; was " + this.maxFetchSize);
			}
			this.maxFetchSize = 1;
		}
		receiver.setMaxFetchSize(this.maxFetchSize);
		receiver.setSelectorExpression(selectorExpression);

		if (isPop3) {
			if (this.isShouldMarkMessagesAsRead() && this.logger.isWarnEnabled()) {
				logger.warn("Setting 'should-mark-messages-as-read' to 'true' while using POP3 has no effect");
			}
		}
		else if (isImap) {
			((ImapMailReceiver) receiver).setShouldMarkMessagesAsRead(this.shouldMarkMessagesAsRead);
		}
		receiver.afterPropertiesSet();
		return receiver;
	}

	public void destroy() throws Exception {
		if (this.receiver != null && this.receiver instanceof DisposableBean) {
			((DisposableBean) this.receiver).destroy();
		}
	}

}
