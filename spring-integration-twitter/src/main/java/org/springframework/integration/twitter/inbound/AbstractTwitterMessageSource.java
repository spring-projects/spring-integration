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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.history.HistoryWritingMessagePostProcessor;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.integration.store.MetadataStore;
import org.springframework.integration.store.SimpleMetadataStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.twitter.core.Tweet;
import org.springframework.integration.twitter.core.TwitterOperations;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract class that defines common operations for receiving various types of
 * messages when using the Twitter API. This class also handles keeping track of
 * the latest inbound message it has received and avoiding, where possible,
 * redelivery of common messages. This functionality is enabled using the
 * {@link org.springframework.integration.store.MetadataStore} 
 * strategy.
 * 
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractTwitterMessageSource<T> extends AbstractEndpoint 
						implements MessageSource, TrackableComponent {
	
	private volatile MetadataStore metadataStore;

	private volatile String metadataKey;

	protected final Queue<Tweet> tweets = new LinkedBlockingQueue<Tweet>();
	
	protected volatile int prefetchThreshold = 0;

	protected volatile long markerId = -1;
	
	protected volatile long processedId = -1;

	protected final TwitterOperations twitter;

	private final Object markerGuard = new Object();

	private volatile ScheduledFuture<?> twitterUpdatePollingTask;

	private final HistoryWritingMessagePostProcessor historyWritingPostProcessor = new HistoryWritingMessagePostProcessor();

	public AbstractTwitterMessageSource(TwitterOperations twitter){
		this.twitter = twitter;
	}

	public void setShouldTrack(boolean shouldTrack) {
		this.historyWritingPostProcessor.setShouldTrack(shouldTrack);
	}

	public long getMarkerId() {
		return this.markerId;
	}

	protected boolean hasMarkedStatus() {
		return this.markerId > -1;
	}

	@Override
	protected void onInit() throws Exception{
		Assert.notNull(this.getTaskScheduler(), 
				"Can not locate TaskScheduler. You must inject one explicitly or define a bean by the name 'taskScheduler'");
		super.onInit();

		if (this.metadataStore == null) {
			// first try to look for a 'messageStore' in the context
			BeanFactory beanFactory = this.getBeanFactory();
			if (beanFactory != null) {
				MetadataStore metadataStore = IntegrationContextUtils.getMetadataStore(beanFactory);
				if (metadataStore != null) {
					this.metadataStore = metadataStore;
				}
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
		String profileId = twitter.getProfileId();
		metadataKeyBuilder.append(profileId);
		this.metadataKey = metadataKeyBuilder.toString();
		String lastId = this.metadataStore.get(this.metadataKey);
		// initialize the last status ID from the metadataStore
		if (StringUtils.hasText(lastId)){
			this.markerId = Long.parseLong(lastId);
		}
	}

	@SuppressWarnings("unchecked")
	protected void forwardAll(List<Tweet> tResponses) {
		Collections.sort(tResponses, this.getComparator());
		for (Tweet twitterResponse : tResponses) {
			forward(twitterResponse);
		}
	}

	abstract Runnable getApiCallback();
	
	protected Comparator getComparator() {
		return new Comparator<Tweet>() {
			public int compare(Tweet tweet1, Tweet tweet2) {
				return tweet1.getCreatedAt().compareTo(tweet2.getCreatedAt());
			}
		};
	}

	@Override
	protected void doStart(){
		Assert.notNull(this.twitter, "'twitter' instance can't be null");
		historyWritingPostProcessor.setTrackableComponent(this);
		// temporarily injecting Twitter into a trigger so it can deal with Rate Limits. will be changed 
		// once we switch to Spring Social
		RateLimitStatusTrigger trigger = new RateLimitStatusTrigger(this.twitter.getUnderlyingTwitter());
		//
		Runnable apiCallback = this.getApiCallback();
		twitterUpdatePollingTask = this.getTaskScheduler().schedule(apiCallback, trigger);
	}

	@Override
	protected void doStop(){
		twitterUpdatePollingTask.cancel(true);
	}

	public Message<?> receive() {
		Tweet tweet = tweets.poll();	
		if (tweet != null){
			this.markProcessedId(tweet.getId());
			return MessageBuilder.withPayload(tweet).build();
		}
		return null;
	}
	
	protected void forward(Tweet tweet) {
		synchronized (this.markerGuard) {
			
			long id = tweet.getId();
			
			if (id > this.markerId) {
				this.markerId = id;
				tweets.add(tweet);
			}
		}
	}

	protected void markProcessedId(long statusId) {
		this.processedId = statusId;
		this.metadataStore.put(this.metadataKey, String.valueOf(statusId));
	}
}
