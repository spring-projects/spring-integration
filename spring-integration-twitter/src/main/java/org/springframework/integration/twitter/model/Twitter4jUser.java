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
package org.springframework.integration.twitter.model;

import twitter4j.User;

import java.net.URL;


/**
 * implementation of the User interfce to represent Users in a Twitter application
 *
 * @author Josh Long
 */
public class Twitter4jUser implements org.springframework.integration.twitter.model.User {

	private twitter4j.User user;

	public Twitter4jUser(User u) {
		this.user = u;
	}

	public int getId() {
		return this.user.getId();
	}

	public String getName() {
		return user.getName();
	}

	public String getScreenName() {
		return this.user.getScreenName();
	}

	public String getLocation() {
		return this.user.getLocation();
	}

	public String getDescription() {
		return user.getDescription();
	}

	public boolean isContributorsEnabled() {
		return user.isContributorsEnabled();
	}

	public URL getProfileImageURL() {
		return user.getProfileImageURL();
	}

	public URL getURL() {
		return this.user.getURL();
	}

	public boolean isProtected() {
		return this.user.isProtected();
	}

	public int getFollowersCount() {
		return this.user.getFollowersCount();
	}
}
