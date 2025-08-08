/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.ignore;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.xmpp.outbound.PresenceSendingMessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests {@link PresenceSendingMessageHandler} to ensure that we are able to publish status.
 *
 * @author Josh Long
 * @author Florian Schmaus
 *
 * @since 2.0
 */
@SpringJUnitConfig
@Disabled
public class OutboundPresenceTests {

	@Autowired
	@Qualifier("outboundPresenceChannel")
	private DirectChannel input;

	@Test
	public void testOutbound() throws Throwable {
		Presence presence = StanzaBuilder.buildPresence().build();
		input.send(new GenericMessage<>(presence));
		Thread.sleep(60 * 1000);
	}

}
