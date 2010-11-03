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

package org.springframework.integration.xmpp.messages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xmpp.XmppHeaders;

/**
 * This component logs in as a user and forwards any messages <em>to</em> that
 * user on to downstream components. The component is an endpoint that has its
 * own lifecycle and does not need any poller
 * to work. It takes any message from a given XMPP session (as established by
 * the current {@link XMPPConnection}) and forwards the
 * {@link org.jivesoftware.smack.packet.Message} as the payload of the Spring
 * Integration {@link org.springframework.integration.Message}. The
 * {@link org.jivesoftware.smack.Chat} instance that's used is passed along as a
 * header (under {@link org.springframework.integration.xmpp.XmppHeaders#CHAT}).
 * Additionally, the {@link org.jivesoftware.smack.packet.Message.Type} is
 * passed along under the header
 * {@link org.springframework.integration.xmpp.XmppHeaders#TYPE}. Both of these
 * pieces of metadata can be obtained directly from the payload, if required.
 * They are here as a convenience.
 * <p/>
 * <strong>Note</strong>: the {@link org.jivesoftware.smack.ChatManager}
 * maintains a Map&lt;String, Chat&gt; for threads and users, where the threadID
 * ({@link String}) is the key or the userID {@link String} is the key. This
 * {@link java.util.Map} is a Smack-specific implementation called
 * {@link org.jivesoftware.smack.util.collections.ReferenceMap} that removes
 * key/values as references are dereferenced. Take care to enable this garbage
 * collection, taking what you need from the payload and the headers and
 * discarding as soon as possible.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @see ChatManager the ChatManager class that
 *      keeps watch over all Chats between the client and any other
 *      participants.
 * @see MessagingTemplate
 *      handles all interesing operations on any Spring Integration channels.
 * @see XMPPConnection the XMPPConnection (as
 *      created by {@link XmppConnectionFactory}
 */
public class XmppMessageDrivenEndpoint extends AbstractEndpoint  {

	private static final Log logger = LogFactory.getLog(XmppMessageDrivenEndpoint.class);

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private volatile MessageChannel requestChannel;

	private volatile XMPPConnection xmppConnection;

	private volatile boolean extractPayload = true;


	/**
	 * This will be injected or configured via a <em>xmpp-connection-factory</em> element.
	 *
	 * @param xmppConnection the connection
	 */
	public void setXmppConnection(final XMPPConnection xmppConnection) {
		this.xmppConnection = xmppConnection;
	}

	/**
	 * @param requestChannel the channel on which the inbound message should be sent
	 */
	public void setRequestChannel(final MessageChannel requestChannel) {
		this.messagingTemplate.setDefaultChannel(requestChannel);
		this.requestChannel = requestChannel;
	}


	/**
	 * Specify whether the text message body should be extracted when mapping to a
	 * Spring Integration Message payload. Otherwise, the full XMPP Message will be
	 * passed within the payload. This value is <em>true</em> by default.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	protected void doStart() {
		logger.debug("start: " + xmppConnection.isConnected() + ":" + xmppConnection.isAuthenticated());
	}

	@Override
	protected void doStop() {
		if (xmppConnection.isConnected()) {
			logger.debug("shutting down " + XmppMessageDrivenEndpoint.class.getName() + ".");
			xmppConnection.disconnect();
		}
	}

	@Override
	protected void onInit() throws Exception {
		messagingTemplate.afterPropertiesSet();
		xmppConnection.addPacketListener(new PacketListener() {
			public void processPacket(final Packet packet) {
				org.jivesoftware.smack.packet.Message message = (org.jivesoftware.smack.packet.Message) packet;
				forwardXmppMessage(xmppConnection.getChatManager().getThreadChat(message.getThread()), message);
			}
		}, null);
	}

	private void forwardXmppMessage(Chat chat, Message xmppMessage) {
		Object payload = (this.extractPayload ? xmppMessage.getBody() : xmppMessage);
		MessageBuilder<?> messageBuilder = MessageBuilder.withPayload(payload)
				.setHeader(XmppHeaders.TYPE, xmppMessage.getType())
				.setHeader(XmppHeaders.CHAT, chat);
		messagingTemplate.send(requestChannel, messageBuilder.build());
	}

}
