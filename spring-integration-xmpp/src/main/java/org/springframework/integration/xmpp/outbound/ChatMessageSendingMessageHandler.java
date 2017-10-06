/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.StringReader;
import java.util.regex.Pattern;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jxmpp.jid.impl.JidCreate;
import org.xmlpull.v1.XmlPullParser;

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
 * (org.jivesoftware.smack.packet.Message) or String.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class ChatMessageSendingMessageHandler extends AbstractXmppConnectionAwareMessageHandler {

	private static final Pattern XML_PATTERN = Pattern.compile("<(\\S[^>\\s]*)[^>]*>[^<]*</\\1>");

	private volatile XmppHeaderMapper headerMapper = new DefaultXmppHeaderMapper();

	private ExtensionElementProvider<? extends ExtensionElement> extensionProvider;

	public ChatMessageSendingMessageHandler() {
		super();
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
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Assert.isTrue(this.initialized, getComponentName() + "#" + this.getComponentType() + " must be initialized");
		Object payload = message.getPayload();
		org.jivesoftware.smack.packet.Message xmppMessage = null;
		if (payload instanceof org.jivesoftware.smack.packet.Message) {
			xmppMessage = (org.jivesoftware.smack.packet.Message) payload;
		}
		else {
			String to = message.getHeaders().get(XmppHeaders.TO, String.class);
			Assert.state(StringUtils.hasText(to), "The '" + XmppHeaders.TO + "' header must not be null");
			xmppMessage = new org.jivesoftware.smack.packet.Message(JidCreate.from(to));

			if (payload instanceof ExtensionElement) {
				xmppMessage.addExtension((ExtensionElement) payload);
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
					XmlPullParser xmlPullParser = PacketParserUtils.newXmppParser(new StringReader(data));
					xmlPullParser.next();
					ExtensionElement extension = this.extensionProvider.parse(xmlPullParser);
					xmppMessage.addExtension(extension);
				}
				else {
					xmppMessage.setBody((String) payload);
				}
			}
			else {
				throw new MessageHandlingException(message,
						"Only payloads of type java.lang.String, org.jivesoftware.smack.packet.Message " +
								"or org.jivesoftware.smack.packet.ExtensionElement " +
								"are supported. Received [" + payload.getClass().getName() +
								"]. Consider adding a Transformer prior to this adapter.");
			}
		}

		if (this.headerMapper != null) {
			this.headerMapper.fromHeadersToRequest(message.getHeaders(), xmppMessage);
		}

		if (!this.xmppConnection.isConnected() && this.xmppConnection instanceof AbstractXMPPConnection) {
			((AbstractXMPPConnection) this.xmppConnection).connect();
		}
		this.xmppConnection.sendStanza(xmppMessage);
	}

}
