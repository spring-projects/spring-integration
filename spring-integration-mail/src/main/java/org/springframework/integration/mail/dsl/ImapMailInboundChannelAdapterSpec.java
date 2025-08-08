/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.mail.dsl;

import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.SearchTermStrategy;

/**
 * A {@link MailInboundChannelAdapterSpec} for IMAP.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ImapMailInboundChannelAdapterSpec
		extends MailInboundChannelAdapterSpec<ImapMailInboundChannelAdapterSpec, ImapMailReceiver> {

	protected ImapMailInboundChannelAdapterSpec() {
		super(new ImapMailReceiver());
	}

	protected ImapMailInboundChannelAdapterSpec(ImapMailReceiver imapMailReceiver) {
		super(imapMailReceiver, true);
	}

	protected ImapMailInboundChannelAdapterSpec(String url) {
		super(new ImapMailReceiver(url), false);
	}

	/**
	 * A {@link SearchTermStrategy} to use.
	 * @param searchTermStrategy the searchTermStrategy.
	 * @return the spec.
	 * @see ImapMailReceiver#setSearchTermStrategy(SearchTermStrategy)
	 */
	public ImapMailInboundChannelAdapterSpec searchTermStrategy(SearchTermStrategy searchTermStrategy) {
		assertReceiver();
		this.receiver.setSearchTermStrategy(searchTermStrategy);
		return this;
	}

	/**
	 * A flag to determine if message should be marked as read.
	 * @param shouldMarkMessagesAsRead the shouldMarkMessagesAsRead.
	 * @return the spec.
	 * @see ImapMailReceiver#setShouldMarkMessagesAsRead(Boolean)
	 */
	public ImapMailInboundChannelAdapterSpec shouldMarkMessagesAsRead(boolean shouldMarkMessagesAsRead) {
		assertReceiver();
		this.receiver.setShouldMarkMessagesAsRead(shouldMarkMessagesAsRead);
		return this;
	}

}
