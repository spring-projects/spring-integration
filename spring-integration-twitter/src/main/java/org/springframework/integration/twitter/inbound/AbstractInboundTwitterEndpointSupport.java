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
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.history.HistoryWritingMessagePostProcessor;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.integration.store.MetadataStore;
import org.springframework.integration.store.SimpleMetadataStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.twitter.oauth.OAuthConfiguration;
import org.springframework.util.Assert;

import twitter4j.DirectMessage;
import twitter4j.Status;
import twitter4j.Twitter;

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
public abstract class AbstractInboundTwitterEndpointSupport<T> extends AbstractEndpoint 
			implements MessageSource, SmartLifecycle, TrackableComponent {
	
	private volatile MetadataStore metadataStore;

	private volatile String metadataKey;

	protected volatile OAuthConfiguration configuration;
	
	protected final Queue<Object> tweets = new LinkedBlockingQueue<Object>();
	
	protected volatile int prefetchThreshold = 0;

	protected volatile long markerId = -1;

	protected Twitter twitter;

	private final Object markerGuard = new Object();

	private volatile ScheduledFuture<?> twitterUpdatePollingTask;

	private final HistoryWritingMessagePostProcessor historyWritingPostProcessor = new HistoryWritingMessagePostProcessor();


	public void setConfiguration(OAuthConfiguration configuration) {
		this.configuration = configuration;
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
		super.onInit();
		Assert.notNull(this.configuration, "'configuration' can't be null");
		this.twitter = this.configuration.getTwitter();
		Assert.notNull(this.twitter, "'twitter' instance can't be null");
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
		Assert.hasText(this.getComponentName(), "Inbound Twitter adapter must have a name");
		this.metadataKey = this.getComponentType() + "." + this.getComponentName() 
				+ "." + this.configuration.getConsumerKey();
	}

	@SuppressWarnings("unchecked")
	protected void forwardAll(List<T> tResponses) {
		Collections.sort(tResponses, this.getComparator());
		for (T twitterResponse : tResponses) {
			forward(twitterResponse);
		}
	}

	abstract Runnable getApiCallback();
	
	protected Comparator getComparator() {
		return new Comparator<Status>() {
			public int compare(Status status, Status status1) {
				return status.getCreatedAt().compareTo(status1.getCreatedAt());
			}
		};
	}

	@Override
	protected void doStart(){
		historyWritingPostProcessor.setTrackableComponent(this);
		RateLimitStatusTrigger trigger = new RateLimitStatusTrigger(this.twitter);
		Runnable apiCallback = this.getApiCallback();
		twitterUpdatePollingTask = this.getTaskScheduler().schedule(apiCallback, trigger);
	}

	@Override
	protected void doStop(){
		twitterUpdatePollingTask.cancel(true);
	}

	@Override
	public Message<?> receive() {
		Object tweet = tweets.poll();
		if (tweet != null){
			return MessageBuilder.withPayload(tweet).build();
		}
		return null;
	}
	
	protected void forward(T tweet) {
		synchronized (this.markerGuard) {
			
			long id = 0;
			if (tweet instanceof DirectMessage) {
				id = ((DirectMessage) tweet).getId();
			}
			else if (tweet instanceof Status) {
				id = ((Status) tweet).getId();
			}
			else {
				throw new IllegalArgumentException("Unsupported type of Twitter message: " + tweet.getClass());
			}
			String lastId = this.metadataStore.get(this.metadataKey);

			long lastTweetId = 0;
			if (lastId != null) {
				lastTweetId = Long.parseLong(lastId);
			}
			if (id > lastTweetId) {
				tweets.add(tweet);
				markLastStatusId(id);
			}
		}
	}

	protected void markLastStatusId(long statusId) {
		this.metadataStore.put(this.metadataKey, String.valueOf(statusId));
	}
}
