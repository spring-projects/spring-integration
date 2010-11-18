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

package org.springframework.integration.twitter.inbound;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.store.MetadataStore;
import org.springframework.integration.store.SimpleMetadataStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.twitter.core.Tweet;
import org.springframework.integration.twitter.core.TwitterOperations;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract class that defines common operations for receiving various types of
 * messages when using the Twitter API. This class also handles keeping track of
 * the latest inbound message it has received and avoiding, where possible,
 * redelivery of duplicate messages. This functionality is enabled using the
 * {@link org.springframework.integration.store.MetadataStore} strategy.
 * 
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
abstract class AbstractTwitterMessageSource<T> extends AbstractEndpoint implements MessageSource {
	
	private volatile long lastPollForTweet;
	
	private final CountDownLatch latch = new CountDownLatch(1);
	
	private final TwitterPollingTask twitterPoller = new TwitterPollingTask();

	private volatile MetadataStore metadataStore;

	private volatile String metadataKey;

	private final Queue<Tweet> tweets = new LinkedBlockingQueue<Tweet>();

	private volatile int prefetchThreshold = 0;

	private volatile long lastEnqueuedId = -1;

	private volatile long lastProcessedId = -1;

	private final TwitterOperations twitterOperations;

	private final TweetComparator tweetComparator = new TweetComparator();

	private volatile ScheduledFuture<?> twitterPollingTask;

	private final Object markerGuard = new Object();


	public AbstractTwitterMessageSource(TwitterOperations twitterOperations) {
		Assert.notNull(twitterOperations, "twitterOperations must not be null");
		this.twitterOperations = twitterOperations;
	}


	protected TwitterOperations getTwitterOperations() {
		return this.twitterOperations;
	}

	@Override
	protected void onInit() throws Exception{
		super.onInit();
		Assert.notNull(this.getTaskScheduler(), 
				"Unable to locate TaskScheduler. You must inject one explicitly or define a bean by the name 'taskScheduler'.");
		if (this.metadataStore == null) {
			// first try to look for a 'metadataStore' in the context
			BeanFactory beanFactory = this.getBeanFactory();
			if (beanFactory != null) {
				this.metadataStore = IntegrationContextUtils.getMetadataStore(beanFactory);
			}
			if (this.metadataStore == null) {
				this.metadataStore = new SimpleMetadataStore();
			}
		}
		StringBuilder metadataKeyBuilder = new StringBuilder();
		if (StringUtils.hasText(this.getComponentType())) {
			metadataKeyBuilder.append(this.getComponentType() + ".");
		}
		if (StringUtils.hasText(this.getComponentName())) {
			metadataKeyBuilder.append(this.getComponentName() + ".");
		}
		else if (logger.isWarnEnabled()) {
			logger.warn(this.getClass().getSimpleName() + " has no name. MetadataStore key might not be unique.");
		}
		String profileId = this.twitterOperations.getProfileId();
		if (profileId != null) {
			metadataKeyBuilder.append(profileId);
		}
		this.metadataKey = metadataKeyBuilder.toString();
		String lastId = this.metadataStore.get(this.metadataKey);
		// initialize the last status ID from the metadataStore
		if (StringUtils.hasText(lastId)) {
			this.lastProcessedId = Long.parseLong(lastId);
			this.lastEnqueuedId = this.lastProcessedId;
		}
	}

	public Message<?> receive() {   
		Tweet tweet = this.tweets.poll();
		if (tweet == null){
			long currentTime = System.currentTimeMillis();
			long diff = currentTime - lastPollForTweet;
			lastPollForTweet = currentTime;
			if (diff < 15000){
				try {
					long waitTime = 15000-diff;
					this.latch.await(waitTime, TimeUnit.MILLISECONDS);
				} catch (Exception e) {
					//ignore
				}
			}
			twitterPoller.run();
		}
		tweet = this.tweets.poll();
		if (tweet != null) {
			this.lastProcessedId = tweet.getId();
			this.metadataStore.put(this.metadataKey, String.valueOf(this.lastProcessedId));
			return MessageBuilder.withPayload(tweet).build();
		}
		return null;
	}

	private void enqueueAll(List<Tweet> tweets) {
		Collections.sort(tweets, this.tweetComparator);
		for (Tweet tweet : tweets) {
			enqueue(tweet);
		}
	}

	private void enqueue(Tweet tweet) {
		synchronized (this.markerGuard) {
			long id = tweet.getId();
			if (id > this.lastEnqueuedId) {
				this.lastEnqueuedId = id;
				this.tweets.add(tweet);
			}
		}
	}


	/**
	 * Subclasses must implement this to return tweets.
	 * The 'sinceId' value will be negative if no last id is known.
	 */
	protected abstract List<Tweet> pollForTweets(long sinceId);


	// Lifecycle methods

	@Override
	protected void doStart() {
		// temporarily injecting Twitter into a trigger so it can deal with Rate Limits.
		// This will likely change once we switch to Spring Social.
		RateLimitStatusTrigger trigger = new RateLimitStatusTrigger(this.twitterOperations.getUnderlyingTwitter());
		this.twitterPollingTask = this.getTaskScheduler().schedule(new TwitterPollingTask(), trigger);
	}

	@Override
	protected void doStop() {
		if (this.twitterPollingTask != null) {
			this.twitterPollingTask.cancel(true);
		}
	}


	private class TwitterPollingTask implements Runnable {

		public void run() {
			try {
				if (tweets.size() <= prefetchThreshold) {
					List<Tweet> tweets = pollForTweets(lastEnqueuedId);
					if (!CollectionUtils.isEmpty(tweets)) {
						enqueueAll(tweets);
					}
				}	
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new MessagingException("failed while polling Twitter", e);
			}
		}
	}


	private static class TweetComparator implements Comparator<Tweet> {

		public int compare(Tweet tweet1, Tweet tweet2) {
			return tweet1.getCreatedAt().compareTo(tweet2.getCreatedAt());
		}
	}

}
