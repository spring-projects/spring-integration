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

package org.springframework.integration.mail;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.mail.monitor.MonitoringStrategy;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;

/**
 * {@link MessageSource} implementation that delegates to a
 * {@link MonitoringStrategy} to poll a mailbox. Each poll of the mailbox may
 * return more than one message which will then be stored in a queue.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class PollingMailSource implements MessageSource<javax.mail.Message> {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final FolderConnection folderConnection;

	private final Queue<javax.mail.Message> mailQueue = new ConcurrentLinkedQueue<javax.mail.Message>();


	public PollingMailSource(FolderConnection folderConnection) {
		Assert.notNull(folderConnection, "folderConnection must not be null");
		this.folderConnection = folderConnection;
	}


	@SuppressWarnings("unchecked")
	public Message<javax.mail.Message> receive() {
		try {
			javax.mail.Message mailMessage = this.mailQueue.poll();
			if (mailMessage == null) {
				javax.mail.Message[] messages = this.folderConnection.receive();
				if (messages != null) {
					for (javax.mail.Message message : messages) {
						this.mailQueue.add(message);
					}
				}
				mailMessage = this.mailQueue.poll();
			}
			if (mailMessage != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Received mail message [" + mailMessage + "]");
				}
				return MessageBuilder.withPayload(mailMessage).build();
			}
		}
		catch (Exception e) {
			throw new MessagingException("failure occurred while polling for mail", e);
		}
		return null;
	}

}
