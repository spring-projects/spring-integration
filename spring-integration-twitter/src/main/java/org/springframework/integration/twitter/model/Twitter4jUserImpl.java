package org.springframework.integration.twitter.model;

import twitter4j.User;

import java.net.URL;


/**
 * implementation of the User interfce to represent Users in a Twitter application
 *
 * @author Josh Long
 */
public class Twitter4jUserImpl implements org.springframework.integration.twitter.model.User {

	private twitter4j.User user;

	public Twitter4jUserImpl(User u) {
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
