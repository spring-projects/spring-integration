/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.ignore;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.messaging.Message;

/**
 * Generates XMPP messages every 2s and forwards them on a channel which in turn publishes the message through XMPP.
 *
 * @author Josh Long
 * @since 2.0
 */
public class XmppMessageProducer implements MessageSource<String> {

	private static final Log logger = LogFactory.getLog(XmppMessageProducer.class);

	private volatile int counter;

	private String recipient;

	public void setRecipient(final String recipient) {
		this.recipient = recipient;
	}

	public Message<String> receive() {
		try {
			if (counter > 10) {
				logger.debug("return null");
				return null;
			}
			counter += 1;
			Thread.sleep(1000 * 2);

			String msg = "the current time is " + new Date();

			logger.info("sending message to recipient " + recipient);

			return MessageBuilder.withPayload(msg).setHeader(XmppHeaders.TO, recipient).build();
		}
		catch (InterruptedException e) {
			logger.debug("exception thrown when trying to receive a message", e);
		}
		return null;
	}

}
