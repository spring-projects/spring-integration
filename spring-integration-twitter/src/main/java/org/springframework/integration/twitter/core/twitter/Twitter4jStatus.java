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

import org.springframework.integration.twitter.core.Status;
import org.springframework.integration.twitter.core.User;

import java.util.Date;

/**
 *
 * An implemention o the {@link org.springframework.integration.twitter.core.Status} interface that
 * forwards requests to a {@link twitter4j.Status} implementation
 *
 * @author Josh Long
 * @since 2.0 
 */
public class Twitter4jStatus implements Status {
	private twitter4j.Status status;

	public Twitter4jStatus(twitter4j.Status s) {
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
		return new Twitter4jUser(this.status.getUser());
	}

	public boolean isRetweet() {
		return this.status.isRetweet();
	}

	public Status getRetweetedStatus() {
		return new Twitter4jStatus(this.status.getRetweetedStatus());
	}

	public String[] getContributors() {
		return this.status.getContributors();
	}
}
