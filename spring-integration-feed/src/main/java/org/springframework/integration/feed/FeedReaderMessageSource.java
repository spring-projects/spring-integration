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

import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherListener;
import com.sun.syndication.fetcher.impl.AbstractFeedFetcher;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HashMapFeedInfoCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;

/**
 * This implementation of {@link MessageSource} will produce a Message whose payload is
 * an instance of {@link SyndFeed} for a feed identified with the 'feedUrl' attribute.
 * 
 * @author Josh Long
 * @author Mario Gray
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public class FeedReaderMessageSource extends IntegrationObjectSupport implements InitializingBean, MessageSource<SyndFeed> {

	private final URL feedUrl;

	private final AbstractFeedFetcher fetcher;

	private volatile FeedFetcherCache fetcherCache;

	private final ConcurrentLinkedQueue<SyndFeed> feeds = new ConcurrentLinkedQueue<SyndFeed>();

	private final Object feedMonitor = new Object();


	public FeedReaderMessageSource(URL feedUrl) {
		this.feedUrl = feedUrl;
		if (feedUrl.getProtocol().equals("file")) {
			this.fetcher = new FileUrlFeedFetcher();
		}
		else if (feedUrl.getProtocol().equals("http")) {
			this.fetcherCache = HashMapFeedInfoCache.getInstance();
			this.fetcher = new HttpURLFeedFetcher(fetcherCache);
		}
		else {
			throw new IllegalArgumentException("Unsupported URL protocol: " + feedUrl.getProtocol());
		}
	}


	URL getFeedUrl() {
		return this.feedUrl;
	}

	@Override
	protected void onInit() throws Exception {
		fetcher.addFetcherEventListener(new FeedQueueUpdatingFetcherListener());
		Assert.notNull(this.feedUrl, "the feedUrl must not be null");
	}

	SyndFeed receiveFeed() {
		SyndFeed feed = null;
		try {
			synchronized (this.feedMonitor) {
				feed = this.fetcher.retrieveFeed(this.feedUrl);
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

	public Message<SyndFeed> receive() {
		SyndFeed feed = this.receiveFeed();
		if (feed == null) {
			return null;
		}
		return MessageBuilder.withPayload(feed).setHeader(FeedConstants.FEED_URL, this.feedUrl).build();
	}


	private class FeedQueueUpdatingFetcherListener implements FetcherListener {

		/**
		 * @see com.sun.syndication.fetcher.FetcherListener#fetcherEvent(com.sun.syndication.fetcher.FetcherEvent)
		 */
		public void fetcherEvent(final FetcherEvent event) {
			String eventType = event.getEventType();
			if (FetcherEvent.EVENT_TYPE_FEED_POLLED.equals(eventType)) {
				logger.debug("\tEVENT: Feed Polled. URL = " + event.getUrlString());
			}
			else if (FetcherEvent.EVENT_TYPE_FEED_RETRIEVED.equals(eventType)) {
				logger.debug("\tEVENT: Feed Retrieved. URL = " + event.getUrlString());
				feeds.add(event.getFeed());
			}
			else if (FetcherEvent.EVENT_TYPE_FEED_UNCHANGED.equals(eventType)) {
				logger.debug("\tEVENT: Feed Unchanged. URL = " + event.getUrlString());
			}
		}
	}

}
