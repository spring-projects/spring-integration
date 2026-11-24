/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mail;

/**
 * {@link org.springframework.integration.core.MessageSource} implementation that
 * delegates to a {@link org.springframework.integration.mail.inbound.MailReceiver} to poll a mailbox.
 * Each poll of the mailbox may return more than one message which will then be stored in a queue.
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Trung Pham
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.mail.inbound.MailReceivingMessageSource}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class MailReceivingMessageSource
		extends org.springframework.integration.mail.inbound.MailReceivingMessageSource {

	public MailReceivingMessageSource(org.springframework.integration.mail.inbound.MailReceiver mailReceiver) {
		super(mailReceiver);
	}

}
