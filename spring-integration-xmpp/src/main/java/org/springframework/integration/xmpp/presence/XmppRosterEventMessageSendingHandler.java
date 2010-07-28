package org.springframework.integration.xmpp.presence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageDeliveryException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.MessageHandlingException;
import org.springframework.integration.core.MessageRejectedException;
import org.springframework.integration.message.OutboundMessageMapper;

/**
 * This class will facilitate publishing updated presence values for a given connection. This change happens on the
 * {@link org.jivesoftware.smack.Roster#setSubscriptionMode(org.jivesoftware.smack.Roster.SubscriptionMode)} property.
 * 
 * @author Josh Long
 * @see org.jivesoftware.smack.packet.Presence.Mode the mode (i.e.:
 * {@link org.jivesoftware.smack.packet.Presence.Mode#away})
 * @see org.jivesoftware.smack.packet.Presence.Type the type (i.e.:
 * {@link org.jivesoftware.smack.packet.Presence.Type#available} )
 * @since 2.0
 */
public class XmppRosterEventMessageSendingHandler implements MessageHandler, Lifecycle {
	private static final Log logger = LogFactory.getLog(XmppRosterEventMessageDrivenEndpoint.class);

	private volatile boolean running;

	private OutboundMessageMapper<Presence> messageMapper;

	private volatile XMPPConnection xmppConnection;

	public void setXmppConnection(final XMPPConnection xmppConnection) {
		this.xmppConnection = xmppConnection;
	}

	public void handleMessage(final Message<?> message) throws MessageRejectedException, MessageHandlingException,
			MessageDeliveryException {
		try {
			Presence presence = this.messageMapper.fromMessage(message);
			this.xmppConnection.sendPacket(presence);
		}
		catch (Exception e) {
			logger.error("Failed to map packet to message ", e);
		}
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		if (null == this.messageMapper) {
			this.messageMapper = new XmppPresenceMessageMapper();
		}

		this.running = true;
	}

	public void stop() {
		this.running = false;

		if (xmppConnection.isConnected()) {
			if (logger.isInfoEnabled()) {
				logger.info("shutting down XMPP connection");
			}

			xmppConnection.disconnect();
		}
	}

	/**
	 * the MessageMapper is responsible for converting outbound Messages into status updates of type
	 * {@link org.jivesoftware.smack.packet.Presence}
	 * @param messageMapper mapper for the message into a {@link Presence} instance
	 */
	public void setMessageMapper(OutboundMessageMapper<Presence> messageMapper) {
		this.messageMapper = messageMapper;
	}
}
