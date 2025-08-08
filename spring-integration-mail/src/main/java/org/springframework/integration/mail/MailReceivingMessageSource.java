/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
