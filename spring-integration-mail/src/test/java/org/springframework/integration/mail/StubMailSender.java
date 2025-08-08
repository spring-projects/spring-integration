/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.mail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

/**
 * @author Artem Bilan
 * @since 4.1.3
 */
public class StubMailSender implements MailSender {

	private final List<SimpleMailMessage> sentMessages = new ArrayList<SimpleMailMessage>();

	@Override
	public void send(SimpleMailMessage simpleMessage) throws MailException {
		this.sentMessages.add(simpleMessage);
	}

	@Override
	public void send(SimpleMailMessage... simpleMessages) throws MailException {
		this.sentMessages.addAll(Arrays.asList(simpleMessages));
	}

	public List<SimpleMailMessage> getSentMessages() {
		return this.sentMessages;
	}

	public void reset() {
		this.sentMessages.clear();
	}

}
