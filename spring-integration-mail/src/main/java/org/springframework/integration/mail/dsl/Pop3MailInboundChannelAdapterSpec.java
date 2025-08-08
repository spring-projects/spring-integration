/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.mail.dsl;

import org.springframework.integration.mail.Pop3MailReceiver;

/**
 * A {@link MailInboundChannelAdapterSpec} for POP3.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 5.0
 */
public class Pop3MailInboundChannelAdapterSpec
		extends MailInboundChannelAdapterSpec<Pop3MailInboundChannelAdapterSpec, Pop3MailReceiver> {

	protected Pop3MailInboundChannelAdapterSpec() {
		super(new Pop3MailReceiver());
	}

	protected Pop3MailInboundChannelAdapterSpec(Pop3MailReceiver receiver) {
		super(receiver, true);
	}

	protected Pop3MailInboundChannelAdapterSpec(String url) {
		super(new Pop3MailReceiver(url));
	}

	protected Pop3MailInboundChannelAdapterSpec(String host, String username, String password) {
		super(new Pop3MailReceiver(host, username, password));
	}

	protected Pop3MailInboundChannelAdapterSpec(String host, int port, String username, String password) {
		super(new Pop3MailReceiver(host, port, username, password));
	}

}
