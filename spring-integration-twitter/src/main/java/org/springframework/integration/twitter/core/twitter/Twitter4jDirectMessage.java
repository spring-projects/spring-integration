/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.twitter.core.twitter;

import org.springframework.integration.twitter.core.User;
import twitter4j.DirectMessage;

import java.util.Date;


/**
 * implementation of the {@link org.springframework.integration.twitter.core.DirectMessage} interface
 * that wraps, and works with, a {@link twitter4j.DirectMessage} instance.
 *
 * @author Josh Long
 * @since 2.0 
 */
public class Twitter4jDirectMessage implements org.springframework.integration.twitter.core.DirectMessage {
	private DirectMessage directMessage;

	public Twitter4jDirectMessage(DirectMessage directMessage) {
		this.directMessage = directMessage;
	}

	public DirectMessage getDirectMessage() {
		return this.directMessage;
	}

	public int getId() {
		return this.directMessage.getId();
	}

	public User getSender() {
		return new Twitter4jUser(this.directMessage.getSender());
	}

	public User getRecipient() {
		return new Twitter4jUser(this.directMessage.getRecipient());
	}

	public String getText() {
		return this.directMessage.getText();
	}

	public int getSenderId() {
		return this.directMessage.getSenderId();
	}

	public int getRecipientId() {
		return this.directMessage.getRecipientId();
	}

	public Date getCreatedAt() {
		return this.directMessage.getCreatedAt();
	}

	public String getSenderScreenName() {
		return this.directMessage.getSenderScreenName();
	}

	public String getRecipientScreenName() {
		return this.directMessage.getRecipientScreenName();
	}
}
