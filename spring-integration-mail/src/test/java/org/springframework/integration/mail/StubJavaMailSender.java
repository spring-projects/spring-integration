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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * @author Marius Bogoevici
 * @author Gary Russell
 */
public class StubJavaMailSender implements JavaMailSender {

	private final MimeMessage uniqueMessage;

	private final List<MimeMessage> sentMimeMessages = new ArrayList<MimeMessage>();

	private final List<SimpleMailMessage> sentSimpleMailMessages = new ArrayList<SimpleMailMessage>();

	public StubJavaMailSender(MimeMessage uniqueMessage) {
		this.uniqueMessage = uniqueMessage;
	}

	public List<MimeMessage> getSentMimeMessages() {
		return this.sentMimeMessages;
	}

	public List<SimpleMailMessage> getSentSimpleMailMessages() {
		return this.sentSimpleMailMessages;
	}

	@Override
	public MimeMessage createMimeMessage() {
		return this.uniqueMessage;
	}

	@Override
	public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
		return this.uniqueMessage;
	}

	@Override
	public void send(MimeMessage mimeMessage) throws MailException {
		this.sentMimeMessages.add(mimeMessage);
	}

	@Override
	public void send(MimeMessage... mimeMessages) throws MailException {
		this.sentMimeMessages.addAll(Arrays.asList(mimeMessages));
	}

	@Override
	public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
		throw new UnsupportedOperationException("MimeMessagePreparator not supported");
	}

	@Override
	public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
		throw new UnsupportedOperationException("MimeMessagePreparator not supported");
	}

	@Override
	public void send(SimpleMailMessage simpleMessage) throws MailException {
		this.sentSimpleMailMessages.add(simpleMessage);
	}

	@Override
	public void send(SimpleMailMessage... simpleMessages) throws MailException {
		this.sentSimpleMailMessages.addAll(Arrays.asList(simpleMessages));
	}

	public void reset() {
		this.sentMimeMessages.clear();
		this.sentSimpleMailMessages.clear();
	}

}
