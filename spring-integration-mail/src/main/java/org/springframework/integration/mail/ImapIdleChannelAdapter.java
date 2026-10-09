/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail;

import jakarta.mail.Message;

/**
 * An event-driven Channel Adapter that receives mail messages from a mail
 * server that supports the IMAP "idle" command (see RFC 2177). Received mail
 * messages will be converted and sent as Spring Integration Messages to the
 * output channel. The Message payload will be the {@link Message}
 * instance that was received.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.mail.inbound.ImapIdleChannelAdapter}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class ImapIdleChannelAdapter extends org.springframework.integration.mail.inbound.ImapIdleChannelAdapter {

	public ImapIdleChannelAdapter(org.springframework.integration.mail.inbound.ImapMailReceiver mailReceiver) {
		super(mailReceiver);
	}

}
