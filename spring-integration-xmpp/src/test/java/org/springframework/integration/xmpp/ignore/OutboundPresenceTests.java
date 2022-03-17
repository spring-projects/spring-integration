/*
 * Copyright 2002-2022 the original author or authors.
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
