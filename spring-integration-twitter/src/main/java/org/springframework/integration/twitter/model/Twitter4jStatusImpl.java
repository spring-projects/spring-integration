package org.springframework.integration.twitter.model;

import java.util.Date;

/**
 *
 * An implemention o the {@link org.springframework.integration.twitter.model.Status} interface that
 * forwards requests to a {@link twitter4j.Status} implementation
 *
 * @author Josh Long
 */
public class Twitter4jStatusImpl implements Status {
	private twitter4j.Status status;

	public Twitter4jStatusImpl(twitter4j.Status s) {
		this.status = s;
	}

	public Date getCreatedAt() {
		return this.status.getCreatedAt();
	}

	public long getId() {
		return this.status.getId();
	}

	public String getText() {
		return this.status.getText();
	}

	public String getSource() {
		return this.status.getSource();
	}

	public boolean isTruncated() {
		return this.status.isTruncated();
	}

	public long getInReplyToStatusId() {
		return this.status.getInReplyToStatusId();
	}

	public int getInReplyToUserId() {
		return this.status.getInReplyToUserId();
	}

	public String getInReplyToScreenName() {
		return this.status.getInReplyToScreenName();
	}

	public boolean isFavorited() {
		return this.status.isFavorited();
	}

	public User getUser() {
		return new Twitter4jUserImpl(this.status.getUser());
	}

	public boolean isRetweet() {
		return this.status.isRetweet();
	}

	public Status getRetweetedStatus() {
		return new Twitter4jStatusImpl(this.status.getRetweetedStatus());
	}

	public String[] getContributors() {
		return this.status.getContributors();
	}
}
