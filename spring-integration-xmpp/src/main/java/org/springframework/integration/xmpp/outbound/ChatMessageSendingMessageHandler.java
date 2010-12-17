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

package org.springframework.integration.xmpp.outbound;

import org.jivesoftware.smack.XMPPConnection;

import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.integration.xmpp.core.AbstractXmppConnectionAwareMessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * MessageHandler that sends an XMPP Chat Message. Supported payload types are Smack Message
 * (org.jivesoftware.smack.packet.Message) or String.
 * 
 * @author Josh Long
 * @author Mario Gray
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ChatMessageSendingMessageHandler extends AbstractXmppConnectionAwareMessageHandler {
	
	public ChatMessageSendingMessageHandler() {
		super();
	}
		
	public ChatMessageSendingMessageHandler(XMPPConnection xmppConnection) {
		super(xmppConnection);
	}


	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Assert.isTrue(this.initialized, this.getComponentName() + "#" + this.getComponentType() + " must be initialized");
		Object messageBody = message.getPayload();
		org.jivesoftware.smack.packet.Message xmppMessage = null;
		if (messageBody instanceof org.jivesoftware.smack.packet.Message) {
			xmppMessage = (org.jivesoftware.smack.packet.Message) messageBody;
		}
		else if (messageBody instanceof String) {
			String chatTo = message.getHeaders().get(XmppHeaders.CHAT_TO, String.class);
			Assert.state(StringUtils.hasText(chatTo), "The '" + XmppHeaders.CHAT_TO + "' header must not be null");
			xmppMessage = new org.jivesoftware.smack.packet.Message(chatTo);
			String threadId = message.getHeaders().get(XmppHeaders.CHAT_THREAD_ID, String.class);
			if (StringUtils.hasText(threadId)) {
				xmppMessage.setThread(threadId);
			}
			xmppMessage.setBody((String) messageBody);
		}
		else {
			throw new MessageHandlingException(message, "Only payloads of type java.lang.String or org.jivesoftware.smack.packet.Message " +
					"are supported. Received [" + messageBody.getClass().getName() + 
					"]. Consider adding a Transformer prior to this adapter.");
		}
		if (!this.xmppConnection.isConnected()) {
			this.xmppConnection.connect();
		}
		this.xmppConnection.sendPacket(xmppMessage);
	}

}
