/*
 * Copyright 2002-present the original author or authors.
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

import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

/**
 * A {@link org.springframework.messaging.MessageHandler} implementation for sending mail.
 *
 * <p>If the Message is an instance of {@link MailMessage}, it will be passed
 * as-is. If the Message payload is a byte array, it will be passed as an
 * attachment, and in that case, the {@link MailHeaders#ATTACHMENT_FILENAME}
 * header is required. Otherwise, a String type is expected, and its content
 * will be used as the text within a {@link SimpleMailMessage}.
 *
 *
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Ma Jiandong
 *
 * @see MailHeaders
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.mail.outbound.MailSendingMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class MailSendingMessageHandler extends org.springframework.integration.mail.outbound.MailSendingMessageHandler {

	/**
	 * Create a MailSendingMessageHandler.
	 * @param mailSender the {@link MailSender} instance to which this
	 * adapter will delegate.
	 */
	public MailSendingMessageHandler(MailSender mailSender) {
		super(mailSender);
	}

}
