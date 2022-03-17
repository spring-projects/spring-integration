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

package org.springframework.integration.xmpp.outbound;

import java.util.regex.Pattern;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.integration.xmpp.core.AbstractXmppConnectionAwareMessageHandler;
import org.springframework.integration.xmpp.support.DefaultXmppHeaderMapper;
import org.springframework.integration.xmpp.support.XmppHeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * MessageHandler that sends an XMPP Chat Message. Supported payload types are Smack Message
 * {@link org.jivesoftware.smack.packet.Message} or String.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Florian Schmaus
 *
 * @since 2.0
 */
public class ChatMessageSendingMessageHandler extends AbstractXmppConnectionAwareMessageHandler {

	private static final Pattern XML_PATTERN = Pattern.compile("<(\\S[^>\\s]*)[^>]*>[^<]*</\\1>");

	private XmppHeaderMapper headerMapper = new DefaultXmppHeaderMapper();

	private ExtensionElementProvider<? extends ExtensionElement> extensionProvider;

	public ChatMessageSendingMessageHandler() {
	}

	public ChatMessageSendingMessageHandler(XMPPConnection xmppConnection) {
		super(xmppConnection);
	}

	public void setHeaderMapper(XmppHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * Specify an {@link ExtensionElementProvider} to build an {@link ExtensionElement}
	 * for the {@link org.jivesoftware.smack.packet.Message#addExtension(ExtensionElement)}
	 * instead of {@code body}.
	 * @param extensionProvider the {@link ExtensionElementProvider} to use.
	 * @since 4.3
	 */
	public void setExtensionProvider(ExtensionElementProvider<? extends ExtensionElement> extensionProvider) {
		this.extensionProvider = extensionProvider;
	}

	@Override
	public String getComponentType() {
		return "xmpp:outbound-channel-adapter";
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		org.jivesoftware.smack.packet.Message xmppMessage;
		Assert.isTrue(isInitialized(),
				() -> getComponentName() + "#" + getComponentType() + " must be initialized");
		try {
			Object payload = message.getPayload();
			MessageBuilder xmppMessageBuilder;
			if (payload instanceof org.jivesoftware.smack.packet.Message) {
				xmppMessage = (org.jivesoftware.smack.packet.Message) payload;
				xmppMessageBuilder = xmppMessage.asBuilder();
			}
			else {
				String to = message.getHeaders().get(XmppHeaders.TO, String.class);
				Assert.state(StringUtils.hasText(to), () -> "The '" + XmppHeaders.TO + "' header must not be null");
				xmppMessageBuilder = buildXmppMessage(payload, to);
			}

			if (this.headerMapper != null) {
				this.headerMapper.fromHeadersToRequest(message.getHeaders(), xmppMessageBuilder);
			}
			XMPPConnection xmppConnection = getXmppConnection();
			if (!xmppConnection.isConnected() && xmppConnection instanceof AbstractXMPPConnection) {
				((AbstractXMPPConnection) xmppConnection).connect();
			}
			xmppMessage = xmppMessageBuilder.build();
			xmppConnection.sendStanza(xmppMessage);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessageHandlingException(message, "Thread interrupted in the [" + this + ']', e);
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "Failed to handle message in the [" + this + ']', e);
		}
	}

	private MessageBuilder buildXmppMessage(Object payload, String to)
			throws Exception { // NOSONAR Smack throws it

		Jid toJid = JidCreate.from(to);
		MessageBuilder xmppMessageBuilder =
				StanzaBuilder.buildMessage()
						.to(toJid);

		if (payload instanceof ExtensionElement extensionElement) {
			xmppMessageBuilder.addExtension(extensionElement);
		}
		else if (payload instanceof String) {
			if (this.extensionProvider != null) {
				String data = (String) payload;
				if (!XML_PATTERN.matcher(data.trim()).matches()) {
					// Since XMPP Extension parsers deal only with XML content,
					// add an arbitrary tag that is removed by the extension parser,
					// if the target content isn't XML.
					data = "<root>" + data + "</root>";
				}
				XmlPullParser xmlPullParser = PacketParserUtils.getParserFor(data);
				ExtensionElement extension = this.extensionProvider.parse(xmlPullParser);
				xmppMessageBuilder.addExtension(extension);
			}
			else {
				String body = (String) payload;
				xmppMessageBuilder.setBody(body);
			}
		}
		else {
			throw new IllegalStateException(
					"Only payloads of type java.lang.String, org.jivesoftware.smack.packet.Message " +
							"or org.jivesoftware.smack.packet.ExtensionElement " +
							"are supported. Received [" + payload.getClass().getName() +
							"]. Consider adding a Transformer prior to this adapter.");
		}
		return xmppMessageBuilder;
	}

}
