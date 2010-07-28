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
import org.jivesoftware.smack.XMPPConnection;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Josh Long
 * @author Mario Gray
 * @since 2.0
 */
public class XmppMessageSendingMessageHandler implements MessageHandler, Lifecycle {

	private static final Log logger = LogFactory.getLog(XmppMessageSendingMessageHandler.class);

	private volatile boolean running;
	private volatile XMPPConnection xmppConnection;

	public void setXmppConnection(final XMPPConnection xmppConnection) {
		this.xmppConnection = xmppConnection;
	}

	public void handleMessage(final org.springframework.integration.core.Message<?> message) {
		try {
			// pre-reqs: user to send, string to send as msg body
			String messageBody = null;
			String destinationUser = null;
			Object payload = message.getPayload();
			if (payload instanceof String) {
				messageBody = (String) payload;
			}
			destinationUser = (String) message.getHeaders().get(XmppHeaders.CHAT_TO_USER);
			Assert.state(StringUtils.hasText(destinationUser), "the destination user must not be null");
			Assert.state(StringUtils.hasText(messageBody), "the message body must not be null");
			String threadId = (String) message.getHeaders().get(XmppHeaders.CHAT_THREAD_ID);
			Chat chat = getOrCreateChatWithParticipant(destinationUser, threadId);
			if (chat != null) {
				chat.sendMessage(messageBody);
			}
		}
		catch (Exception e) {
			logger.debug("failed to send XMPP message", e);
		}
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
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

	private Chat getOrCreateChatWithParticipant(String userId, String thread) {
		Chat chat = null;
		if (!StringUtils.hasText(thread)) {
			chat = xmppConnection.getChatManager().createChat(userId, null);
		}
		else {
			chat = xmppConnection.getChatManager().getThreadChat(thread);
			if (chat == null) {
				chat = xmppConnection.getChatManager().createChat(userId, thread, null);
			}
		}
		return chat;
	}

}
