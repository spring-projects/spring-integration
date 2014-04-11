/* Copyright 2002-2014 the original author or authors.
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
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.social.twitter.api.DirectMessage;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.UserOperations;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract class that defines common operations for receiving various types of
 * messages when using the Twitter API. This class also handles keeping track of
 * the latest inbound message it has received and avoiding, where possible,
 * redelivery of duplicate messages. This functionality is enabled using the
 * {@link org.springframework.integration.metadata.MetadataStore} strategy.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
abstract class AbstractTwitterMessageSource<T> extends IntegrationObjectSupport implements MessageSource {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private final Twitter twitter;

	private final TweetComparator tweetComparator = new TweetComparator();

	private final Object lastEnqueuedIdMonitor = new Object();

	private final String metadataKey;

	private volatile MetadataStore metadataStore;

	private final Queue<T> tweets = new LinkedBlockingQueue<T>();

	private volatile int prefetchThreshold = 0;

	private volatile long lastEnqueuedId = -1;

	private volatile long lastProcessedId = -1;

	private volatile int pageSize = DEFAULT_PAGE_SIZE;


	public AbstractTwitterMessageSource(Twitter twitter, String metadataKey) {
		Assert.notNull(twitter, "twitter must not be null");
		Assert.notNull(metadataKey, "metadataKey must not be null");
		this.twitter = twitter;
		if (this.twitter.isAuthorized()){
			UserOperations userOperations = this.twitter.userOperations();
			String profileId = String.valueOf(userOperations.getProfileId());
			if (profileId != null) {
				metadataKey += "." + profileId;
			}
		}
		this.metadataKey = metadataKey;
	}


	public void setMetadataStore(MetadataStore metadataStore) {
		this.metadataStore = metadataStore;
	}

	public void setPrefetchThreshold(int prefetchThreshold) {
		this.prefetchThreshold = prefetchThreshold;
	}

	protected Twitter getTwitter() {
		return this.twitter;
	}

	protected int getPageSize() {
		return this.pageSize;
	}

	/**
	 * Set the limit for the number of results returned on each poll; default 20.
	 * @param pageSize The pageSize.
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
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

		String lastId = this.metadataStore.get(this.metadataKey);
		// initialize the last status ID from the metadataStore
		if (StringUtils.hasText(lastId)) {
			this.lastProcessedId = Long.parseLong(lastId);
		    this.lastEnqueuedId = this.lastProcessedId;
		}

	}

	@Override
	public Message<?> receive() {
		T tweet = this.tweets.poll();
		if (tweet == null) {
			this.refreshTweetQueueIfNecessary();
			tweet = this.tweets.poll();
		}

		if (tweet != null) {
			this.lastProcessedId = this.getIdForTweet(tweet);
			this.metadataStore.put(this.metadataKey, String.valueOf(this.lastProcessedId));
			return this.getMessageBuilderFactory().withPayload(tweet).build();
		}
		return null;
	}

	private void enqueueAll(List<T> tweets) {
		Collections.sort(tweets, this.tweetComparator);
		for (T tweet : tweets) {
			enqueue(tweet);
		}
	}

	private void enqueue(T tweet) {
		synchronized (this.lastEnqueuedIdMonitor) {
			long id = this.getIdForTweet(tweet);
			if (id > this.lastEnqueuedId) {
				this.tweets.add(tweet);
				this.lastEnqueuedId = id;
			}
		}
	}

	private void refreshTweetQueueIfNecessary() {
		try {
			if (tweets.size() <= prefetchThreshold) {
				List<T> tweets = pollForTweets(lastEnqueuedId);
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

	/**
	 * Subclasses must implement this to return tweets.
	 * The 'sinceId' value will be negative if no last id is known.
	 *
	 * @param sinceId The id of the last reported tweet.
	 * @return The list of tweets.
	 */
	protected abstract List<T> pollForTweets(long sinceId);


	private long getIdForTweet(T twitterMessage) {
		if (twitterMessage instanceof Tweet) {
			return ((Tweet) twitterMessage).getId();
		}
		else if (twitterMessage instanceof DirectMessage) {
			return ((DirectMessage) twitterMessage).getId();
		}
		else {
			throw new IllegalArgumentException("Unsupported Twitter object: " + twitterMessage);
		}
	}


	/**
	 * Remove the metadata key and the corresponding value from the Metadata Store.
	 */
	@ManagedOperation(description="Remove the metadata key and the corresponding value from the Metadata Store.")
	void resetMetadataStore() {
		synchronized(this) {
			this.metadataStore.remove(this.metadataKey);
			this.lastProcessedId = -1L;
			this.lastEnqueuedId = -1L;
		}
	}

	/**
	 *
	 * @return {@code -1} if lastProcessedId is not set, yet.
	 */
	@ManagedAttribute
	public long getLastProcessedId() {
		return this.lastProcessedId;
	}

	private class TweetComparator implements Comparator<T> {

		@Override
		public int compare(T tweet1, T tweet2) {
			// hopefully temporary logic. Will suggest that SpringSocial use a common base class for DM and Tweet
			if (tweet1 instanceof Tweet && tweet2 instanceof Tweet) {
				Tweet t1 = (Tweet) tweet1;
				Tweet t2 = (Tweet) tweet2;
				Date t1CreatedAt = t1.getCreatedAt();
				Date t2CreatedAt = t2.getCreatedAt();
				Assert.notNull(t1CreatedAt, "Tweet is missing 'createdAt' date. Cannot compare.");
				Assert.notNull(t2CreatedAt, "Tweet is missing 'createdAt' date. Cannot compare.");
				return t1CreatedAt.compareTo(t2CreatedAt);
			}
			else if (tweet1 instanceof DirectMessage && tweet2 instanceof DirectMessage) {
				DirectMessage d1 = (DirectMessage) tweet1;
				DirectMessage d2 = (DirectMessage) tweet2;
				Date d1CreatedAt = d1.getCreatedAt();
				Date d2CreatedAt = d2.getCreatedAt();
				Assert.notNull(d1CreatedAt, "DirectMessage is missing 'createdAt' date. Cannot compare.");
				Assert.notNull(d2CreatedAt, "DirectMessage is missing 'createdAt' date. Cannot compare.");
				return d1CreatedAt.compareTo(d2CreatedAt);
			}
			else {
				throw new IllegalArgumentException("Uncomparable Twitter objects: " + tweet1 + " and " + tweet2);
			}
		}

	}

}
