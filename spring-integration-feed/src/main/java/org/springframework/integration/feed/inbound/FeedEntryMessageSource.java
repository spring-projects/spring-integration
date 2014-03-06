/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.feed.inbound;

import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherListener;
import com.sun.syndication.fetcher.impl.HashMapFeedInfoCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;

/**
 * This implementation of {@link MessageSource} will produce individual
 * {@link SyndEntry}s for a feed identified with the 'feedUrl' attribute.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class FeedEntryMessageSource extends IntegrationObjectSupport implements MessageSource<SyndEntry> {

	private final URL feedUrl;

	private final FeedFetcher feedFetcher;

	private final Queue<SyndEntry> entries = new ConcurrentLinkedQueue<SyndEntry>();

	private final String metadataKey;

	private volatile MetadataStore metadataStore;

	private volatile long lastTime = -1;

	private volatile boolean initialized;

	private final Object monitor = new Object();

	private final Comparator<SyndEntry> syndEntryComparator = new SyndEntryPublishedDateComparator();

	private final Object feedMonitor = new Object();


	/**
	 * Creates a FeedEntryMessageSource that will use a HttpURLFeedFetcher to read feeds from the given URL.
	 * If the feed URL has a protocol other than http*, consider providing a custom implementation of the
	 * {@link FeedFetcher} via the alternate constructor.
	 *
	 * @param feedUrl The URL.
	 * @param metadataKey The metadata key.
	 */
	public FeedEntryMessageSource(URL feedUrl, String metadataKey) {
		this(feedUrl, metadataKey, new HttpURLFeedFetcher(HashMapFeedInfoCache.getInstance()));
	}

	/**
	 * Creates a FeedEntryMessageSource that will use the provided FeedFetcher to read from the given feed URL.
	 *
	 * @param feedUrl The URL.
	 * @param metadataKey The metadata key.
	 * @param feedFetcher The feed fetcher.
	 */
	public FeedEntryMessageSource(URL feedUrl, String metadataKey, FeedFetcher feedFetcher) {
		Assert.notNull(feedUrl, "feedUrl must not be null");
		Assert.notNull(metadataKey, "metadataKey must not be null");
		Assert.notNull(feedFetcher, "feedFetcher must not be null");
		this.feedUrl = feedUrl;
		this.metadataKey = metadataKey + "." + this.feedUrl;
		this.feedFetcher = feedFetcher;
	}


	public void setMetadataStore(MetadataStore metadataStore) {
		Assert.notNull(metadataStore, "metadataStore must not be null");
		this.metadataStore = metadataStore;
	}

	@Override
	public String getComponentType() {
		return "feed:inbound-channel-adapter";
	}

	@Override
	public Message<SyndEntry> receive() {
		Assert.isTrue(this.initialized, "'FeedEntryReaderMessageSource' must be initialized before it can produce Messages.");
		SyndEntry entry = doReceive();
		if (entry == null) {
			return null;
		}
		return this.getMessageBuilderFactory().withPayload(entry).build();
	}

	@Override
	protected void onInit() throws Exception {
		this.feedFetcher.addFetcherEventListener(new FeedQueueUpdatingFetcherListener());
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

		String lastTimeValue = this.metadataStore.get(this.metadataKey);
		if (StringUtils.hasText(lastTimeValue)) {
			this.lastTime = Long.parseLong(lastTimeValue);
		}
		this.initialized = true;
	}

	private SyndEntry doReceive() {
		SyndEntry nextEntry = null;
		synchronized (this.monitor) {
			nextEntry = getNextEntry();
			if (nextEntry == null) {
				// read feed and try again
				this.populateEntryList();
				nextEntry = getNextEntry();
			}
		}
		return nextEntry;
	}

	private SyndEntry getNextEntry() {
		SyndEntry next = this.entries.poll();
		if (next == null) {
			return null;
		}
		if (next.getPublishedDate() != null) {
			this.lastTime = next.getPublishedDate().getTime();
		}
		else {
			this.lastTime += 1;
		}
		this.metadataStore.put(this.metadataKey, this.lastTime + "");
		return next;
	}

	@SuppressWarnings("unchecked")
	private void populateEntryList() {
		SyndFeed syndFeed = this.getFeed();
		if (syndFeed != null) {
			List<SyndEntry> retrievedEntries = syndFeed.getEntries();
			if (!CollectionUtils.isEmpty(retrievedEntries)) {
				boolean withinNewEntries = false;
				Collections.sort(retrievedEntries, this.syndEntryComparator);
				for (SyndEntry entry : retrievedEntries) {
					Date entryDate = getLastModifiedDate(entry);
					if ((entryDate != null && entryDate.getTime() > this.lastTime)
						|| (entryDate == null && withinNewEntries)) {
						this.entries.add(entry);
						withinNewEntries = true;
					}
				}
			}
		}
	}

	private SyndFeed getFeed() {
		SyndFeed feed = null;
		try {
			synchronized (this.feedMonitor) {
				feed = this.feedFetcher.retrieveFeed(this.feedUrl);
				if (logger.isDebugEnabled()) {
					logger.debug("retrieved feed at url '" + this.feedUrl + "'");
				}
				if (feed == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("no feeds updated, returning null");
					}
				}
			}
		}
		catch (Exception e) {
			throw new MessagingException(
					"Failed to retrieve feed at url '" + this.feedUrl + "'", e);
		}
		return feed;
	}

	private static Date getLastModifiedDate(SyndEntry entry) {
		return (entry.getUpdatedDate() != null) ? entry.getUpdatedDate() : entry.getPublishedDate();
	}


	private static class SyndEntryPublishedDateComparator implements Comparator<SyndEntry> {

		@Override
		public int compare(SyndEntry entry1, SyndEntry entry2) {
			Date date1 = getLastModifiedDate(entry1);
			Date date2 = getLastModifiedDate(entry2);
			if (date1 != null && date2 != null) {
				return date1.compareTo(date2);
			}
			if (date1 == null && date2 == null) {
				return 0;
			}
			return (date2 == null) ? 1 : 0;
		}

	}


	private class FeedQueueUpdatingFetcherListener implements FetcherListener {

		/**
		 * @see com.sun.syndication.fetcher.FetcherListener#fetcherEvent(com.sun.syndication.fetcher.FetcherEvent)
		 */
		@Override
		public void fetcherEvent(final FetcherEvent event) {
			String eventType = event.getEventType();
			if (FetcherEvent.EVENT_TYPE_FEED_POLLED.equals(eventType)) {
				if (logger.isDebugEnabled()) {
					logger.debug("\tEVENT: Feed Polled. URL = " + event.getUrlString());
				}
			}
			else if (FetcherEvent.EVENT_TYPE_FEED_UNCHANGED.equals(eventType)) {
				if (logger.isDebugEnabled()) {
					logger.debug("\tEVENT: Feed Unchanged. URL = " + event.getUrlString());
				}
			}
		}

	}

}
