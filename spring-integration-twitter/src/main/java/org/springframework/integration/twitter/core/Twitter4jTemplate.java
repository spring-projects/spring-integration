/**
 * 
 */
package org.springframework.integration.twitter.core;

import java.util.List;

import org.springframework.util.Assert;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 *
 */
public class Twitter4jTemplate implements TwitterOperations{
	private final Twitter twitter;
	/**
	 * Used to construct this template to perform Twitter API calls that do not require authorization.
	 * (e.g., search)
	 */
	public Twitter4jTemplate(){
		this.twitter = new TwitterFactory().getInstance();
	}
	/**
	 * Used to construct this template with OAuth authentication/authorization to perform Twitter API calls
	 * that do require authorization (e.g., send/receive DirectMessage)
	 * 
	 * @param consumerKey
	 * @param consumerSecret
	 * @param accessToken
	 * @param accessTokenSecret
	 */
	public Twitter4jTemplate(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret){
		Assert.hasText(consumerKey, "'consumerKey' must be provided");
		Assert.hasText(consumerSecret, "'consumerSecret' must be provided");
		Assert.hasText(accessToken, "'accessToken' must be provided");
		Assert.hasText(accessTokenSecret, "'accessTokenSecret' must be provided");
		AccessToken at = new AccessToken(accessToken, accessTokenSecret);
		this.twitter = new TwitterFactory().getOAuthAuthorizedInstance(consumerKey, consumerSecret, at);
	}
	
	@Override
	public String getProfileId() {
		try {
			return twitter.getScreenName();
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to obtain profile id ", e);
		} 
	}
	@Override
	public RateLimitStatus getRateLimitStatus() {
		try {
			return twitter.getRateLimitStatus();
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to obtain Rate Limit status ", e);
		}
	}
	@Override
	public List<DirectMessage> getDirectMessages() {
		try {
			return twitter.getDirectMessages();
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Direct Messages ", e);
		}
	}
	@Override
	public List<DirectMessage> getDirectMessages(Paging paging) {
		try {
			return twitter.getDirectMessages(paging);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Direct Messages ", e);
		}
	}
	@Override
	public List<Status> getMentions() {
		try {
			return twitter.getMentions();
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Mention statuses ", e);
		}
	}
	@Override
	public List<Status> getMentions(Paging paging) {
		try {
			return twitter.getMentions(paging);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Mention statuses ", e);
		}
	}
	@Override
	public List<Status> getFriendsTimeline() {
		try {
			return twitter.getFriendsTimeline();
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Timeline statuses ", e);
		}
	}
	@Override
	public List<Status> getFriendsTimeline(Paging paging) {
		try {
			return twitter.getFriendsTimeline(paging);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Timeline statuses ", e);
		}
	}
	@Override
	public void sendDirectMessage(String userName, String text) {
		try {
			twitter.sendDirectMessage(userName, text);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to send Direct Message ", e);
		}
	}
	@Override
	public void sendDirectMessage(int userId, String text) {
		try {
			twitter.sendDirectMessage(userId, text);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to send Direct Message ", e);
		}
	}
	@Override
	public void updateStatus(StatusUpdate status) {
		try {
			twitter.updateStatus(status);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to send Status update ", e);
		}
	}
}
