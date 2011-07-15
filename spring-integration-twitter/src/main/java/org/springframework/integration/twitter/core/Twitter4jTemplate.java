/*
 * Copyright 2002-2011 the original author or authors
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
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;

/**
 * Implementation of {@link TwitterOperations} that delegates to Twitter4J.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class Twitter4jTemplate implements TwitterOperations {

	private final Twitter twitter;


	/**
	 * Used to construct this template to perform Twitter API calls that do not require authorization.
	 * (e.g., search)
	 */
	public Twitter4jTemplate() {
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
	@SuppressWarnings("deprecation")
	public Twitter4jTemplate(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
		Assert.hasText(consumerKey, "'consumerKey' must be provided");
		Assert.hasText(consumerSecret, "'consumerSecret' must be provided");
		Assert.hasText(accessToken, "'accessToken' must be provided");
		Assert.hasText(accessTokenSecret, "'accessTokenSecret' must be provided");
		
		AccessToken token = new AccessToken(accessToken, accessTokenSecret);
		this.twitter = new TwitterFactory().getOAuthAuthorizedInstance(consumerKey, consumerSecret, token);
		/*
		 * We are aware of the fact that the above method is deprecated and the code should really look 
		 * like the one below, but we are keeping the deprecated call to address backwards compatibility.
		 * In future versions we won't be relying on Twitter4J in favor of SpringSocial API.
		 */
//		Properties properties = new Properties();
//		properties.put("oauth.consumerKey", consumerKey);
//		properties.put("oauth.consumerSecret", consumerSecret);
//		Configuration configuration = new PropertyConfiguration(properties);
//		this.twitter = new TwitterFactory(configuration).getInstance(token);
	}


	public String getProfileId() {
		try {
			if (twitter.isOAuthEnabled()) {
				return twitter.getScreenName();
			}
			else {
				return "twitter-anonymous";
			}
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to obtain Profile ID. ", e);
		} 
	}

	public List<Tweet> getDirectMessages() {
		try {
			ResponseList<DirectMessage> directMessages = twitter.getDirectMessages();
			return this.buildTweetsFromTwitterResponses(directMessages);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Direct Messages. ", e);
		}
	}

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

	public List<Tweet> getMentions() {
		try {
			ResponseList<Status> mentions = twitter.getMentions();
			return this.buildTweetsFromTwitterResponses(mentions);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Mention statuses. ", e);
		}
	}

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

	public List<Tweet> getTimeline() {
		try {
			ResponseList<Status> timelines = twitter.getHomeTimeline();
			return this.buildTweetsFromTwitterResponses(timelines);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Timeline statuses. ", e);
		}
	}

	public List<Tweet> getTimeline(long sinceId) {
		try {
			ResponseList<Status> timelines = twitter.getHomeTimeline(new Paging(sinceId));
			return this.buildTweetsFromTwitterResponses(timelines);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to receive Timeline statuses since the last status with ID: " 
					+ sinceId + ".", e);
		}
	}

	public void sendDirectMessage(String userName, String text) {
		Assert.hasText(userName, "'userName' is required");
		Assert.hasText(text, "'text' is required");
		try {
			twitter.sendDirectMessage(userName, text);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to send Direct Message to user: " + userName + ".", e);
		}
	}

	public void sendDirectMessage(int userId, String text) {
		Assert.state(userId > 0, "'userId' is required");
		Assert.hasText(text, "'text' is required");
		try {
			twitter.sendDirectMessage(userId, text);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to send Direct Message to user with id: " + userId + ".", e);
		}
	}

	public void updateStatus(String statusTweet) {
		Assert.hasText(statusTweet, "'statusTweet' must not be null");
		try {
			StatusUpdate status = new StatusUpdate(statusTweet);
			twitter.updateStatus(status);
		} 
		catch (Exception e) {
			throw new TwitterOperationException("Failed to send Status update. ", e);
		}
	}

	public SearchResults search(String query) {
		Assert.hasText(query, "'query' must not be null");
		Query q = new Query(query);	
		return this.search(q);
	}

	public SearchResults search(String query, long sinceId) {
		Assert.hasText(query, "'query' must not be null");
		Query q = new Query(query);
		q.setSinceId(sinceId);
		return this.search(q);
	}

	public Twitter getUnderlyingTwitter() {
		return this.twitter;
	}


	private SearchResults search(Query query) {
		try {
			QueryResult result = twitter.search(query);
			if (result != null) {
				List<twitter4j.Tweet> t4jTweets = result.getTweets();
				List<Tweet> tweets = this.buildTweetsFromTwitterResponses(t4jTweets);
				SearchResults results = new SearchResults(tweets, result.getMaxId(), result.getSinceId());
				return results;
			}
		}
		catch (Exception e) {
			throw new TwitterOperationException("failed to perform Twitter search", e);
		}
		return null;
	}

	private List<Tweet> buildTweetsFromTwitterResponses(List<?> responses) {
		List<Tweet> tweets = new LinkedList<Tweet>();
		if (responses != null) {
			for (Object response : responses) {
				if (response instanceof Status) {
					tweets.add(this.buildTweetFromStatus((Status) response));
				}
				else if (response instanceof DirectMessage) {
					tweets.add(this.buildTweetFromDm((DirectMessage) response));
				}
				else if (response instanceof twitter4j.Tweet) {
					tweets.add(this.buildTweetFromTwitter4jTweet((twitter4j.Tweet) response));
				}
				else {
					throw new TwitterOperationException("Unsupported response type: " + response.getClass());
				}
			}
		}
		return tweets;
	}

	private Tweet buildTweetFromDm(DirectMessage dm) {
		Tweet tweet = new Tweet();
		tweet.setCreatedAt(dm.getCreatedAt());
		tweet.setFromUser(dm.getSenderScreenName());
		tweet.setFromUserId(dm.getSenderId());
		tweet.setId(dm.getId());
		tweet.setText(dm.getText());
		tweet.setToUserId((long)dm.getRecipientId());
		return tweet;
	}

	private Tweet buildTweetFromStatus(Status status) {
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

	private Tweet buildTweetFromTwitter4jTweet(twitter4j.Tweet t4jTweet) {
		Tweet tweet = new Tweet();
		tweet.setCreatedAt(t4jTweet.getCreatedAt());
		tweet.setFromUser(t4jTweet.getFromUser());
		tweet.setFromUserId(t4jTweet.getFromUserId());
		tweet.setId(t4jTweet.getId());
		tweet.setLanguageCode(t4jTweet.getIsoLanguageCode());
		tweet.setProfileImageUrl(t4jTweet.getProfileImageUrl());
		tweet.setSource(t4jTweet.getSource());
		tweet.setText(t4jTweet.getText());
		return tweet;
	}

}
