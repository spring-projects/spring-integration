/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
