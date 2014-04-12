/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.xmpp.outbound;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;

import org.springframework.integration.xmpp.core.AbstractXmppConnectionAwareMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * MessageHandler that publishes updated Presence values for a given XMPP connection.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class PresenceSendingMessageHandler extends AbstractXmppConnectionAwareMessageHandler  {

	public PresenceSendingMessageHandler() {
		super();
	}

	public PresenceSendingMessageHandler(XMPPConnection xmppConnection) {
		super(xmppConnection);
	}

	@Override
	public String getComponentType() {
		return "xmpp:presence-outbound-channel-adapter";
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Assert.isTrue(this.initialized, this.getComponentName() + " must be initialized");
		Object payload = message.getPayload();
		Assert.isTrue(payload instanceof Presence,
				"Payload must be of type 'org.jivesoftware.smack.packet.Presence', was: " + payload.getClass().getName());
		if (!this.xmppConnection.isConnected()) {
			this.xmppConnection.connect();
		}
		this.xmppConnection.sendPacket((Presence) payload);
	}

}
