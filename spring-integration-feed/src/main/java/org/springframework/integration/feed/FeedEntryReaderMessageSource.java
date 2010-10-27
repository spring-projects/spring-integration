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

package org.springframework.integration.feed;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.context.metadata.MetadataStore;
import org.springframework.integration.context.metadata.SimpleMetadataStore;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * This implementation of {@link MessageSource} will produce individual
 * {@link SyndEntry}s for a feed identified with the 'feedUrl' attribute.
 * 
 * @author Josh Long
 * @author Mario Gray
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class FeedEntryReaderMessageSource extends IntegrationObjectSupport implements MessageSource<SyndEntry> {

	private final Queue<SyndEntry> entries = new ConcurrentLinkedQueue<SyndEntry>();

	private final FeedReaderMessageSource feedReaderMessageSource;

	private volatile String metadataKey;

	private volatile MetadataStore metadataStore;

	private volatile long lastTime = -1;

	private volatile boolean initialized;

	private final Object monitor = new Object();

	private final Comparator<SyndEntry> syndEntryComparator = new SyndEntryComparator();


	public FeedEntryReaderMessageSource(FeedReaderMessageSource feedReaderMessageSource) {
		Assert.notNull(feedReaderMessageSource, "'feedReaderMessageSource' must not be null");
		this.feedReaderMessageSource = feedReaderMessageSource;
	}


	public void setMetadataStore(MetadataStore metadataStore) {
		Assert.notNull(metadataStore, "metadataStore must not be null");
		this.metadataStore = metadataStore;
	}

	public String getComponentType() {
		return "feed:inbound-channel-adapter";
	}

	public Message<SyndEntry> receive() {
		Assert.isTrue(this.initialized, "'FeedEntryReaderMessageSource' must be initialized before it can produce Messages.");
		SyndEntry entry = doReceive();
		if (entry == null) {
			return null;
		}
		return MessageBuilder.withPayload(entry).build();
	}

	@Override
	protected void onInit() throws Exception {
		if (this.metadataStore == null) {
			// first try to look for a 'messageStore' in the context
			BeanFactory beanFactory = this.getBeanFactory();
			if (beanFactory != null) {
				this.metadataStore = IntegrationContextUtils.getMetadataStore(beanFactory);
			}
			// if no 'messageStore' in context, fall back to in-memory Map-based default
			if (this.metadataStore == null) {
				this.metadataStore = new SimpleMetadataStore();
			}
		}
		Assert.hasText(this.getComponentName(), "FeedEntryReaderMessageSource must have a name");
		this.metadataKey = this.getComponentType() + "." + this.getComponentName() 
					+ "." + this.feedReaderMessageSource.getFeedUrl();
		String lastTimeValue = this.metadataStore.get(this.metadataKey);
		if (StringUtils.hasText(lastTimeValue)) {
			this.lastTime = Long.parseLong(lastTimeValue);
		}
		this.initialized = true;
	}

	@SuppressWarnings("unchecked")
	private SyndEntry doReceive() {
		SyndEntry nextUp = null;
		synchronized (this.monitor) {
			nextUp = pollAndCache();
			if (nextUp != null) {
				return nextUp;
			}
			// otherwise, fill the backlog
			SyndFeed syndFeed = this.feedReaderMessageSource.receiveSyndFeed();
			if (syndFeed != null) {
				List<SyndEntry> feedEntries = (List<SyndEntry>) syndFeed.getEntries();
				if (null != feedEntries) {
					Collections.sort(feedEntries, syndEntryComparator);
					for (SyndEntry se : feedEntries) {
						long publishedTime = se.getPublishedDate().getTime();
						if (publishedTime > this.lastTime) {
							entries.add(se);
						}
					}
				}
			}
			nextUp = pollAndCache();
		}
		return nextUp;
	}

	private SyndEntry pollAndCache() {
		SyndEntry next = this.entries.poll();
		if (next == null) {
			return null;
		}
		this.lastTime = next.getPublishedDate().getTime();
		this.metadataStore.put(this.metadataKey, this.lastTime + "");
		return next;
	}


	private static class SyndEntryComparator implements Comparator<SyndEntry> {

		public int compare(SyndEntry entry1, SyndEntry entry2) {
			return entry1.getPublishedDate().compareTo(entry2.getPublishedDate());
		}
	}

}
