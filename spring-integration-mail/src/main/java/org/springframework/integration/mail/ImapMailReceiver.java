/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail;

import org.eclipse.angus.mail.imap.IMAPFolder;
import org.jspecify.annotations.Nullable;

/**
 * A {@link org.springframework.integration.mail.inbound.MailReceiver} implementation for receiving mail messages from a
 * mail server that supports the IMAP protocol. In addition to the pollable
 * {@link #receive()} method, the {@link #waitForNewMessages()} method provides
 * the option of blocking until new messages are available prior to calling
 * {@link #receive()}. That option is only available if the server supports
 * the {@link IMAPFolder#idle() idle} command.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Alexander Pinske
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.mail.inbound.ImapMailReceiver}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class ImapMailReceiver extends org.springframework.integration.mail.inbound.ImapMailReceiver {

	public ImapMailReceiver() {
	}

	public ImapMailReceiver(@Nullable String url) {
		super(url);
	}

}
