/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.feed.inbound;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.Resource;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * This implementation of {@link org.springframework.integration.core.MessageSource} will
 * produce individual {@link SyndEntry}s for a feed identified with the 'feedUrl'
 * attribute.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Aaron Loes
 * @author Christian Tzolov
 *
 * @since 2.0
 */
public class FeedEntryMessageSource extends AbstractMessageSource<SyndEntry> {

	private final URL feedUrl;

	private final Resource feedResource;

	private final String metadataKey;

	private final Queue<SyndEntry> entries = new ConcurrentLinkedQueue<>();

	private final Lock monitor = new ReentrantLock();

	private final Comparator<SyndEntry> syndEntryComparator =
			Comparator.comparing(FeedEntryMessageSource::getLastModifiedDate,
					Comparator.nullsFirst(Comparator.naturalOrder()));

	private final Lock feedMonitor = new ReentrantLock();

	private SyndFeedInput syndFeedInput = new SyndFeedInput();

	private boolean syndFeedInputSet;

	private MetadataStore metadataStore;

	private volatile long lastTime = -1;

	private volatile boolean initialized;

	/**
	 * Creates a FeedEntryMessageSource that will use a HttpURLFeedFetcher to read feeds from the given URL.
	 * If the feed URL has a protocol other than http*, consider providing a custom implementation of the
	 * {@link Resource} via the alternate constructor.
	 * @param feedUrl The URL.
	 * @param metadataKey The metadata key.
	 */
	public FeedEntryMessageSource(URL feedUrl, String metadataKey) {
		Assert.notNull(feedUrl, "'feedUrl' must not be null");
		Assert.hasText(metadataKey, "'metadataKey' must not be empty");
		this.feedUrl = feedUrl;
		this.metadataKey = metadataKey;
		this.feedResource = null;
	}

	/**
	 * Creates a FeedEntryMessageSource that will read feeds from the given {@link Resource}.
	 * @param feedResource the {@link Resource} to use.
	 * @param metadataKey the metadata key.
	 * @since 5.0
	 */
	public FeedEntryMessageSource(Resource feedResource, String metadataKey) {
		Assert.notNull(feedResource, "'feedResource' must not be null");
		Assert.hasText(metadataKey, "'metadataKey' must not be empty");
		this.feedResource = feedResource;
		this.metadataKey = metadataKey;
		this.feedUrl = null;
	}

	public void setMetadataStore(MetadataStore metadataStore) {
		Assert.notNull(metadataStore, "'metadataStore' must not be null");
		this.metadataStore = metadataStore;
	}

	/**
	 * Specify a parser for Feed XML documents.
	 * @param syndFeedInput the {@link SyndFeedInput} to use.
	 * @since 5.0
	 */
	public void setSyndFeedInput(SyndFeedInput syndFeedInput) {
		Assert.notNull(syndFeedInput, "'syndFeedInput' must not be null");
		this.syndFeedInput = syndFeedInput;
		this.syndFeedInputSet = true;
	}

	/**
	 * Specify a flag to indication if {@code WireFeed} should be preserved in the target {@link SyndFeed}.
	 * @param preserveWireFeed the {@code boolean} flag.
	 * @since 5.0
	 * @see SyndFeedInput#setPreserveWireFeed(boolean)
	 */
	public void setPreserveWireFeed(boolean preserveWireFeed) {
		Assert.isTrue(!this.syndFeedInputSet,
				() -> "'preserveWireFeed' must be configured on the provided [" + this.syndFeedInput + "]");
		this.syndFeedInput.setPreserveWireFeed(preserveWireFeed);
	}

	@Override
	public String getComponentType() {
		return "feed:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		if (this.metadataStore == null) {
			// first try to look for a 'messageStore' in the context
			BeanFactory beanFactory = getBeanFactory();
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

	@Override
	protected SyndEntry doReceive() {
		Assert.isTrue(this.initialized,
				"'FeedEntryReaderMessageSource' must be initialized before it can produce Messages.");
		SyndEntry nextEntry;
		this.monitor.lock();
		try {
			nextEntry = getNextEntry();
			if (nextEntry == null) {
				// read feed and try again
				populateEntryList();
				nextEntry = getNextEntry();
			}
		}
		finally {
			this.monitor.unlock();
		}
		return nextEntry;
	}

	private SyndEntry getNextEntry() {
		SyndEntry next = this.entries.poll();
		if (next == null) {
			return null;
		}

		Date lastModifiedDate = FeedEntryMessageSource.getLastModifiedDate(next);
		if (lastModifiedDate != null) {
			this.lastTime = lastModifiedDate.getTime();
		}
		else {
			this.lastTime += 1; //NOSONAR - single poller thread
		}
		this.metadataStore.put(this.metadataKey, this.lastTime + "");
		return next;
	}

	private void populateEntryList() {
		SyndFeed syndFeed = this.getFeed();
		if (syndFeed != null) {
			List<SyndEntry> retrievedEntries = syndFeed.getEntries();
			if (!CollectionUtils.isEmpty(retrievedEntries)) {
				boolean withinNewEntries = false;
				retrievedEntries.sort(this.syndEntryComparator);
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
		try {
			this.feedMonitor.lock();
			try {
				SyndFeed feed = buildSyndFeed();
				logger.debug(() -> "Retrieved feed for [" + this + "]");
				if (feed == null) {
					logger.debug(() -> "No feeds updated for [" + this + "], returning null");
				}
				return feed;
			}
			finally {
				this.feedMonitor.unlock();
			}
		}
		catch (Exception e) {
			throw new MessagingException("Failed to retrieve feed for '" + this + "'", e);
		}
	}

	private SyndFeed buildSyndFeed() throws IOException, URISyntaxException, InterruptedException, FeedException {
		InputStream inputStream;
		if (this.feedResource != null) {
			inputStream = this.feedResource.getInputStream();
		}
		else {
			HttpRequest request =
					HttpRequest.newBuilder()
							.GET()
							.uri(this.feedUrl.toURI())
							.build();

			inputStream =
					HttpClient.newHttpClient()
							.send(request, HttpResponse.BodyHandlers.ofInputStream())
							.body();
		}

		try (inputStream) {
			return this.syndFeedInput.build(new XmlReader(inputStream));
		}
	}

	@Override
	public String toString() {
		return "FeedEntryMessageSource{" +
				"feedUrl=" + this.feedUrl +
				", feedResource=" + this.feedResource +
				", metadataKey='" + this.metadataKey + '\'' +
				", lastTime=" + this.lastTime +
				'}';
	}

	private static Date getLastModifiedDate(SyndEntry entry) {
		return (entry.getUpdatedDate() != null) ? entry.getUpdatedDate() : entry.getPublishedDate();
	}

}
