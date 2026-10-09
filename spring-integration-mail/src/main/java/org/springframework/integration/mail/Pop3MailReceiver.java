/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail;

import org.jspecify.annotations.Nullable;

/**
 * A {@link org.springframework.integration.mail.inbound.MailReceiver} implementation that polls a mail server using the
 * POP3 protocol.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.mail.inbound.Pop3MailReceiver}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class Pop3MailReceiver extends org.springframework.integration.mail.inbound.Pop3MailReceiver {

	public Pop3MailReceiver() {
	}

	public Pop3MailReceiver(@Nullable String url) {
		super(url);
	}

	public Pop3MailReceiver(String host, String username, String password) {
		super(host, username, password);
	}

	public Pop3MailReceiver(String host, int port, String username, String password) {
		super(host, port, username, password);
	}

}
