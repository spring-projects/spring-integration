package org.springframework.integration.twitter.model;

import twitter4j.DirectMessage;

import java.util.Date;


/**
 * implementation of the {@link org.springframework.integration.twitter.model.DirectMessage} interface
 * that wraps, and works with, a {@link twitter4j.DirectMessage} instance.
 *
 * @author Josh Long
 */
public class Twitter4jDirectMessageImpl implements org.springframework.integration.twitter.model.DirectMessage {
	private DirectMessage directMessage;

	public Twitter4jDirectMessageImpl(DirectMessage directMessage) {
		this.directMessage = directMessage;
	}

	public DirectMessage getDirectMessage() {
		return this.directMessage;
	}

	public int getId() {
		return this.directMessage.getId();
	}

	public User getSender() {
		return new Twitter4jUserImpl(this.directMessage.getSender());
	}

	public User getRecipient() {
		return new Twitter4jUserImpl(this.directMessage.getRecipient());
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
