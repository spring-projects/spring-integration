/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.mail.AbstractMailReceiver;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.MailReceiver;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 1.0.3
 */
public class MailReceiverFactoryBean implements FactoryBean, DisposableBean {

	private volatile String storeUri;

	private volatile MailReceiver receiver;

	private volatile Properties javaMailProperties;

	private volatile int maxFetchSize = 1;


	public void setStoreUri(String storeUri) {
		this.storeUri = storeUri;
	}

	public void setJavaMailProperties(Properties javaMailProperties) {
		this.javaMailProperties = javaMailProperties;
	}

	public void setMaxFetchSize(int maxFetchSize) {
		this.maxFetchSize = maxFetchSize;
	}

	public Object getObject() throws Exception {
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

	private MailReceiver createReceiver() {
		Assert.hasText(this.storeUri, "the store URI is required");
		boolean isPop3 = this.storeUri.toLowerCase().startsWith("pop3");
		boolean isImap = this.storeUri.toLowerCase().startsWith("imap");
		Assert.isTrue(isPop3 || isImap, "the store URI must begin with 'pop3' or 'imap'");
		AbstractMailReceiver receiver = isPop3 ? new Pop3MailReceiver(this.storeUri) : new ImapMailReceiver(this.storeUri);
		if (this.javaMailProperties != null) {
			receiver.setJavaMailProperties(this.javaMailProperties);
		}
		receiver.setMaxFetchSize(this.maxFetchSize);
		return receiver;
	}

	public void destroy() throws Exception {
		if (this.receiver != null && this.receiver instanceof DisposableBean) {
			((DisposableBean) this.receiver).destroy();
		}
	}

}
