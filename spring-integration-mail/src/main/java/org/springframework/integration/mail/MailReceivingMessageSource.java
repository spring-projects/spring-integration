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

package org.springframework.integration.mail;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.core.log.LogMessage;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.integration.core.MessageSource} implementation that
 * delegates to a {@link MailReceiver} to poll a mailbox. Each poll of the mailbox may
 * return more than one message which will then be stored in a queue.
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Trung Pham
 */
public class MailReceivingMessageSource extends AbstractMessageSource<Object> {

	private final MailReceiver mailReceiver;

	private final Queue<Object> mailQueue = new ConcurrentLinkedQueue<>();

	public MailReceivingMessageSource(MailReceiver mailReceiver) {
		Assert.notNull(mailReceiver, "mailReceiver must not be null");
		this.mailReceiver = mailReceiver;
	}

	@Override
	public String getComponentType() {
		return "mail:inbound-channel-adapter";
	}

	@Override
	protected Object doReceive() {
		try {
			Object mailMessage = this.mailQueue.poll();
			if (mailMessage == null) {
				Object[] messages = this.mailReceiver.receive();
				if (messages != null) {
					this.mailQueue.addAll(Arrays.asList(messages));
				}
				mailMessage = this.mailQueue.poll();
			}
			if (mailMessage != null) {
				this.logger.debug(LogMessage.format("received mail message [%s]", mailMessage));
				return mailMessage;
			}
		}
		catch (Exception e) {
			throw new MessagingException("failure occurred while polling for mail", e);
		}
		return null;
	}

}
