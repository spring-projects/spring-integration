/*
 * Copyright 2002-2010 the original author or authors
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
package org.springframework.integration.twitter.core;

import java.util.LinkedList;
import java.util.List;

import org.springframework.util.Assert;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.ResponseList;
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
			throw new TwitterOperationException("Failed to obtain Profile ID. ", e);
		} 
	}
	
	@Override
	public List<Tweet> getDirectMessages() {
		
		try {
			ResponseList<DirectMessage> directMessages = twitter.getDirectMessages();
			return this.buildTweetsFromTwitterResponses(directMessages);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Direct Messages. ", e);
		}
	}
	@Override
	public List<Tweet> getDirectMessages(long sinceId) {
		try {
			ResponseList<DirectMessage> directMessages = twitter.getDirectMessages(new Paging(sinceId));
			return this.buildTweetsFromTwitterResponses(directMessages);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Direct Messages since the last message with ID: " 
					+ sinceId + ".", e);
		}
	}
	@Override
	public List<Tweet> getMentions() {
		try {
			ResponseList<Status> mentions = twitter.getMentions();
			return this.buildTweetsFromTwitterResponses(mentions);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Mention statuses. ", e);
		}
	}
	@Override
	public List<Tweet> getMentions(long sinceId) {
		try {
			ResponseList<Status> mentions = twitter.getMentions(new Paging(sinceId));
			return this.buildTweetsFromTwitterResponses(mentions);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Mention statuses since the last status with ID: " 
					+ sinceId + ".", e);
		}
	}
	@Override
	public List<Tweet> getHomeTimeline() {
		try {
			ResponseList<Status> timelines = twitter.getHomeTimeline();
			return this.buildTweetsFromTwitterResponses(timelines);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Timeline statuses. ", e);
		}
	}
	@Override
	public List<Tweet> getHomeTimeline(long sinceId) {
		try {
			ResponseList<Status> timelines = twitter.getHomeTimeline(new Paging(sinceId));
			return this.buildTweetsFromTwitterResponses(timelines);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Timeline statuses since the last status with ID: " 
					+ sinceId + ".", e);
		}
	}
	@Override
	public void sendDirectMessage(String userName, String text) {
		Assert.hasText(userName, "'userName' must be set");
		Assert.hasText(text, "'text' must be set");
		try {
			twitter.sendDirectMessage(userName, text);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to send Direct Message to user: " + userName + ".", e);
		}
	}
	@Override
	public void sendDirectMessage(int userId, String text) {
		Assert.state(userId > 0, "'userId' msut be provided");
		Assert.hasText(text, "'text' must be set");
		try {
			twitter.sendDirectMessage(userId, text);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to send Direct Message to user with id: " + userId + ".", e);
		}
	}
	
	@Override
	public void updateStatus(Tweet statusTweet) {
		Assert.notNull(statusTweet, "'statusTweet' must not be null");
		try {
			StatusUpdate status = new StatusUpdate(statusTweet.getText());
			if (statusTweet.getToUserId() != null){
				status.setInReplyToStatusId(statusTweet.getToUserId());
			}
			twitter.updateStatus(status);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to send Status update. ", e);
		}
	}
	
	public Twitter getUnderlyingTwitter(){
		return this.twitter;
	}
	
	private List<Tweet> buildTweetsFromTwitterResponses(List<?> responses){
		List<Tweet> tweets = new LinkedList<Tweet>();
		if (responses != null){
			for (Object response : responses) {
				if (response instanceof Status){
					tweets.add(this.buildTweetFromStatus((Status) response));
				}
				else {
					tweets.add(this.buildTweetFromDm((DirectMessage) response));
				}
			}
		}
		return tweets;
	}
	
	private Tweet buildTweetFromDm(DirectMessage dm){
		Tweet tweet = new Tweet();
		tweet.setCreatedAt(dm.getCreatedAt());
		tweet.setFromUser(dm.getSenderScreenName());
		tweet.setFromUserId(dm.getSenderId());
		tweet.setId(dm.getId());
		tweet.setText(dm.getText());
		tweet.setToUserId((long)dm.getRecipientId());
		return tweet;
	}
	
	private Tweet buildTweetFromStatus(Status status){
		Tweet tweet = new Tweet();
		tweet.setCreatedAt(status.getCreatedAt());
		if (status.getUser() != null){
			tweet.setFromUser(status.getUser().getScreenName());
			tweet.setFromUserId(status.getUser().getId());
		}
		tweet.setId(status.getId());
		tweet.setSource(status.getSource());
		tweet.setText(status.getText());
		return tweet;
	}
}
