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

package org.springframework.integration.xmpp.inbound;

import java.util.Map;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;

import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.xmpp.core.AbstractXmppConnectionAwareEndpoint;
import org.springframework.integration.xmpp.support.DefaultXmppHeaderMapper;
import org.springframework.integration.xmpp.support.XmppHeaderMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This component logs in as a user and forwards any messages <em>to</em> that
 * user on to downstream components.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ChatMessageListeningEndpoint extends AbstractXmppConnectionAwareEndpoint {

	private volatile boolean extractPayload = true;

	private final PacketListener packetListener = new ChatMessagePublishingPacketListener();

	private volatile XmppHeaderMapper headerMapper = new DefaultXmppHeaderMapper();

	public ChatMessageListeningEndpoint() {
		super();
	}

	public ChatMessageListeningEndpoint(XMPPConnection xmppConnection) {
		super(xmppConnection);
	}

	public void setHeaderMapper(XmppHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}


	/**
	 * Specify whether the text message body should be extracted when mapping to a
	 * Spring Integration Message payload. Otherwise, the full XMPP Message will be
	 * passed within the payload. This value is <em>true</em> by default.
	 *
	 * @param extractPayload true if the payload should be extracted.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	public String getComponentType() {
		return "xmpp:inbound-channel-adapter";
	}

	@Override
	protected void doStart() {
		Assert.isTrue(this.initialized, this.getComponentName() + " [" + this.getComponentType() + "] must be initialized");
		this.xmppConnection.addPacketListener(this.packetListener, null);
	}

	@Override
	protected void doStop() {
		if (this.xmppConnection != null) {
			this.xmppConnection.removePacketListener(this.packetListener);
		}
	}


	private class ChatMessagePublishingPacketListener implements PacketListener {

		@Override
		public void processPacket(final Packet packet) {
			if (packet instanceof org.jivesoftware.smack.packet.Message) {
				org.jivesoftware.smack.packet.Message xmppMessage = (org.jivesoftware.smack.packet.Message) packet;
				Map<String, ?> mappedHeaders = headerMapper.toHeadersFromRequest(xmppMessage);

				String messageBody = xmppMessage.getBody();
				/*
				 * Since there are several types of chat messages with different ChatState (e.g., composing, paused etc)
				 * we need to perform further validation since for now we only support messages that have
				 * content (e.g., Use A says 'Hello' to User B). We don't yet support messages with no
				 * content (e.g., User A is typing a message for User B etc.).
				 * See https://jira.springsource.org/browse/INT-1728
				 * Also see: packet.getExtensions()
				 */
				if (StringUtils.hasText(messageBody)){
					Object payload = (extractPayload ? messageBody : xmppMessage);

					AbstractIntegrationMessageBuilder<?> messageBuilder =
							ChatMessageListeningEndpoint.this.getMessageBuilderFactory()
								.withPayload(payload)
								.copyHeaders(mappedHeaders);
					sendMessage(messageBuilder.build());
				}
			}
		}
	}

}
