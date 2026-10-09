/*
 * Copyright 2002-present the original author or authors.
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
