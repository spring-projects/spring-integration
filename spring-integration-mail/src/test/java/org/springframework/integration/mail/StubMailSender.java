/*
 * Copyright 2015-2019 the original author or authors.
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
