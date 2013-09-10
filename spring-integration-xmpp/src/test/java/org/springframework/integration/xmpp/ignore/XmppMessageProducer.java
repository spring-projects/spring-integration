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

package org.springframework.integration.xmpp.ignore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xmpp.XmppHeaders;

import java.util.Date;

/**
 * Generates XMPP messages every 2s and forwards them on a channel which in turn publishes the message through XMPP.
 *
 * @author Josh Long
 * @since 2.0
 */
public class XmppMessageProducer implements MessageSource<String> {

	private static final Log logger = LogFactory.getLog(XmppMessageProducer.class);

	private volatile int counter;
	private String recipient;

	public void setRecipient(final String recipient) {
		this.recipient = recipient;
	}

	public Message<String> receive() {
		try {
			if (counter > 10) {
				logger.debug("return null");
				return null;
			}
			counter += 1;
			Thread.sleep(1000 * 2);

			String msg = "the current time is " + new Date();

			logger.info("sending message to recipient " + recipient);

			return MessageBuilder.withPayload(msg).setHeader(XmppHeaders.TO, recipient).build();
		}
		catch (InterruptedException e) {
			logger.debug("exception thrown when trying to receive a message", e);
		}
		return null;
	}

}
