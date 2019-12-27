/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.xmpp.outbound;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;

import org.springframework.integration.xmpp.core.AbstractXmppConnectionAwareMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;

/**
 * MessageHandler that publishes updated Presence values for a given XMPP connection.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class PresenceSendingMessageHandler extends AbstractXmppConnectionAwareMessageHandler {

	public PresenceSendingMessageHandler() {
	}

	public PresenceSendingMessageHandler(XMPPConnection xmppConnection) {
		super(xmppConnection);
	}

	@Override
	public String getComponentType() {
		return "xmpp:presence-outbound-channel-adapter";
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Assert.state(isInitialized(), () -> getComponentName() + " must be initialized");
		Object payload = message.getPayload();
		Assert.state(payload instanceof Presence,
				() -> "Payload must be of type 'org.jivesoftware.smack.packet.Presence', was: " +
						payload.getClass().getName());
		try {
			XMPPConnection xmppConnection = getXmppConnection();
			if (!xmppConnection.isConnected() && xmppConnection instanceof AbstractXMPPConnection) {
				((AbstractXMPPConnection) xmppConnection).connect();
			}
			xmppConnection.sendStanza((Presence) payload);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessageHandlingException(message, "Thread interrupted in the [" + this + ']', e);
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "Failed to handle message in the [" + this + ']', e);
		}
	}

}
