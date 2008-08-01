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

package org.springframework.integration.adapter.mail;

import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.adapter.mail.monitor.DefaultLocalMailMessageStore;
import org.springframework.integration.adapter.mail.monitor.LocalMailMessageStore;
import org.springframework.integration.adapter.mail.monitor.MonitoringStrategy;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.PollableSource;

/**
 * {@link MessageSource} implementation which delegates to a
 * {@link MonitoringStrategy} to poll a mailbox. Each poll of the mailbox may
 * return more than one message which will then be stored locally using the
 * provided {@link LocalMailMessageStore}
 * 
 * @author Jonas Partner
 */
@SuppressWarnings("unchecked")
public class PollingMailSource implements PollableSource {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final FolderConnection folderConnection;

	private MailMessageConverter converter = new DefaultMailMessageConverter();

	private LocalMailMessageStore mailMessageStore = new DefaultLocalMailMessageStore();

	public PollingMailSource(FolderConnection folderConnetion) {
		this.folderConnection = folderConnetion;
	}

	@SuppressWarnings("unchecked")
	public Message receive() {
		Message received = null;
		javax.mail.Message mailMessage = mailMessageStore.getNext();
		if (mailMessage == null) {
			try {
				javax.mail.Message[] messages = folderConnection.receive();
				mailMessageStore.addLast(messages);
				mailMessage = mailMessageStore.getNext();
			} catch (Exception e) {
				throw new org.springframework.integration.message.MessagingException(
						"Excpetion receiving mail", e);
			}
		}
		if (mailMessage != null) {
			received = converter.create((MimeMessage) mailMessage);
			if (logger.isDebugEnabled()) {
				logger.debug("Received message " + received);
			}
		}
		return received;
	}

	public void setConverter(MailMessageConverter converter) {
		this.converter = converter;
	}

	public void setMailMessageStore(LocalMailMessageStore mailMessageStore) {
		this.mailMessageStore = mailMessageStore;
	}

}
