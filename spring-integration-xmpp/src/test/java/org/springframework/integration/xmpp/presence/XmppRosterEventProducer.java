/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xmpp.presence;

import org.apache.commons.lang.StringUtils;

import org.jivesoftware.smack.packet.Presence;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.message.*;
import org.springframework.integration.xmpp.XmppHeaders;

/**
 * This is used in
 * {@link org.springframework.integration.xmpp.presence.OutboundXmppRosterEventsEndpointTests}
 * to produce fake status / presence updates.
 * 
 * @author Josh Long
 * @since 2.0
 */
public class XmppRosterEventProducer implements MessageSource<String> {

	public Message<String> receive() {
		try {
			Thread.sleep(1000 * 10);
		}
		catch (InterruptedException e) {
			// eat it
		}
		return (Math.random() > .5) ? MessageBuilder.withPayload(StringUtils.EMPTY).setHeader(
				XmppHeaders.PRESENCE_MODE, Presence.Mode.chat).setHeader(XmppHeaders.PRESENCE_TYPE,
				Presence.Type.available).setHeader(XmppHeaders.PRESENCE_STATUS, "She Loves me").build()
				: MessageBuilder.withPayload(StringUtils.EMPTY).setHeader(XmppHeaders.PRESENCE_MODE, Presence.Mode.dnd)
						.setHeader(XmppHeaders.PRESENCE_TYPE, Presence.Type.available).setHeader(
								XmppHeaders.PRESENCE_STATUS, "She Loves me not").build();
	}

}
