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
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.metadata.MetadataStore;
import org.springframework.integration.context.metadata.SimpleMetadataStore;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.history.HistoryWritingMessagePostProcessor;
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
 * {@link org.springframework.integration.context.metadata.MetadataStore}
 * strategy.
 * 
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class AbstractInboundTwitterEndpointSupport<T> extends MessageProducerSupport {

	private volatile MetadataStore metadataStore;

	private volatile String metadataKey;

	protected volatile OAuthConfiguration configuration;

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
	protected void onInit() {
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
			String lastId = this.metadataStore.get(this.metadataKey);

			long lastTweetId = 0;
			if (lastId != null) {
				lastTweetId = Long.parseLong(lastId);
			}
			if (id > lastTweetId) {
				sendMessage(twtMsg);
				markLastStatusId(id);
			}
		}
	}

	protected void markLastStatusId(long statusId) {
		this.metadataStore.put(this.metadataKey, String.valueOf(statusId));
	}

}
