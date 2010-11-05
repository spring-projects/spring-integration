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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.util.Assert;

/**
 * This class will facilitate publishing updated presence values for a given connection. This change happens on the
 * {@link org.jivesoftware.smack.Roster#setSubscriptionMode(org.jivesoftware.smack.Roster.SubscriptionMode)} property.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @see org.jivesoftware.smack.packet.Presence.Mode the mode (i.e.:
 *      {@link org.jivesoftware.smack.packet.Presence.Mode#away})
 * @see org.jivesoftware.smack.packet.Presence.Type the type (i.e.:
 *      {@link org.jivesoftware.smack.packet.Presence.Type#available} )
 * @since 2.0
 */
public class XmppRosterEventMessageSendingHandler extends AbstractMessageHandler  {
	
	private final XMPPConnection xmppConnection;
	
	public XmppRosterEventMessageSendingHandler(XMPPConnection xmppConnection){
		Assert.notNull(xmppConnection, "'xmppConnection' must not be null");
		this.xmppConnection = xmppConnection;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object payload = message.getPayload();
		Assert.isInstanceOf(Presence.class, payload, "'payload' must be of type 'org.jivesoftware.smack.packet.Presence', was " 
					+ payload.getClass().getName());
		this.xmppConnection.sendPacket((Presence)payload);
	}
}
