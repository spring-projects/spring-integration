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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;

import org.springframework.integration.Message;
import org.springframework.integration.context.metadata.FileBasedPropertiesStore;
import org.springframework.integration.context.metadata.MetadataStore;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.history.HistoryWritingMessagePostProcessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.twitter.oauth.OAuthConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import twitter4j.DirectMessage;
import twitter4j.Status;
import twitter4j.Twitter;

/**
 * Abstract class that defines common operations for receiving various types of
 * messages when using the Twitter API. This class also handles keeping track of
 * the latest inbound message it has received and avoiding, where possible,
 * redelivery of common messages. This functionality is enabled using the
 * {@link org.springframework.integration.context.metadata.MetadataStore}
 * strategy.
 * 
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public abstract class AbstractInboundTwitterEndpointSupport<T> extends MessageProducerSupport {

	private volatile MetadataStore metadataStore;

	private volatile String metadataKey;

	private volatile Properties lastPersistentEntry = new Properties();

	protected volatile OAuthConfiguration configuration;

	protected volatile long markerId = -1;

	protected Twitter twitter;

	private final Object markerGuard = new Object();

	private volatile ScheduledFuture<?> twitterUpdatePollingTask;

	private volatile String persistentIdentifier;

	private final HistoryWritingMessagePostProcessor historyWritingPostProcessor = new HistoryWritingMessagePostProcessor();


	public void setPersistentIdentifier(String persistentIdentifier) {
		this.persistentIdentifier = persistentIdentifier;
	}

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
	protected void onInit() {
		super.onInit();
		Assert.notNull(this.configuration, "'configuration' can't be null");
		this.twitter = this.configuration.getTwitter();
		Assert.notNull(this.twitter, "'twitter' instance can't be null");
		metadataKey = this.getComponentType() + "@" + this.getComponentName()
				+ "#" + this.configuration.getConsumerKey();
		try {
			if (StringUtils.hasText(this.persistentIdentifier)) {
				if (this.metadataStore == null) {
					logger.info("Creating FileBasedPropertiesStore");
					metadataStore = new FileBasedPropertiesStore(this.persistentIdentifier);
					((FileBasedPropertiesStore) metadataStore).afterPropertiesSet();
				}
				lastPersistentEntry = metadataStore.load();
			}
		}
		catch (Exception e) {
			logger.warn("Failed to initialize and load from MetadataStore. Potential duplicates possible.", e);
		}
	}

	protected void forwardAll(List<T> tResponses) {
		List<T> stats = new ArrayList<T>();
		for (T t : tResponses) {
			stats.add(t);
		}
		for (T twitterResponse : this.sort(stats)) {
			forward(twitterResponse);
		}
	}

	abstract protected List<T> sort(List<T> rl);

	abstract Runnable getApiCallback();

	@Override
	protected void doStart() {
		historyWritingPostProcessor.setTrackableComponent(this);
		RateLimitStatusTrigger trigger = new RateLimitStatusTrigger(this.twitter);
		Runnable apiCallback = this.getApiCallback();
		twitterUpdatePollingTask = this.getTaskScheduler().schedule(apiCallback, trigger);
	}

	@Override
	protected void doStop() {
		twitterUpdatePollingTask.cancel(true);
	}

	protected void forward(T message) {
		synchronized (this.markerGuard) {
			Message<T> twtMsg = MessageBuilder.withPayload(message).build();

			long id = 0;
			if (message instanceof DirectMessage) {
				id = ((DirectMessage) message).getId();
			}
			else if (message instanceof Status) {
				id = ((Status) message).getId();
			}
			else {
				throw new IllegalArgumentException("Unsupported type of Twitter message: " + message.getClass());
			}
			String lastId = lastPersistentEntry.getProperty(this.metadataKey);

			long lastTweetId = 0;
			if (lastId != null) {
				lastTweetId = Long.parseLong(lastId);
			}
			if (id > lastTweetId) {
				sendMessage(twtMsg);
				markLastStatusId(id);
				if (metadataStore != null) {
					metadataStore.write(this.lastPersistentEntry);
				}
			}
		}
	}

	protected void markLastStatusId(long statusId) {
		lastPersistentEntry.put(metadataKey, String.valueOf(statusId));
	}

}
