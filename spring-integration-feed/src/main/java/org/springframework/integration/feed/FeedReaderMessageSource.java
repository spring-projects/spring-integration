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
import org.springframework.context.Lifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherListener;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HashMapFeedInfoCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;


/**
 * This implementation of {@link MessageSource} will produce {@link SyndFeed} for a feed identified 
 * with 'feedUrl' attribute.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Oleg Zhurakousky
 */
class FeedReaderMessageSource extends IntegrationObjectSupport
        implements InitializingBean, Lifecycle, MessageSource<SyndFeed> {
   
	private volatile boolean running;
    private volatile String feedUrl;
    private volatile URL feedURLObject;
    private volatile FeedFetcherCache fetcherCache;
    private volatile HttpURLFeedFetcher fetcher;
    private volatile ConcurrentLinkedQueue<SyndFeed> syndFeeds;
    private volatile MyFetcherListener myFetcherListener;
    private final Object syndFeedMonitor = new Object();
   
    public FeedReaderMessageSource() {
        syndFeeds = new ConcurrentLinkedQueue<SyndFeed>();
    }
    
    public void setFeedUrl(final String feedUrl) {
        this.feedUrl = feedUrl;
    }
    
    public String getFeedUrl() {
        return feedUrl;
    }
    
    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }
    
    public boolean isRunning() {
        return this.running;
    }

    public SyndFeed receiveSyndFeed() {
        SyndFeed returnedSyndFeed = null;

        try {
            synchronized (syndFeedMonitor) {
            	returnedSyndFeed = fetcher.retrieveFeed(this.feedURLObject);
                logger.debug("attempted to retrieve feed '" + this.feedUrl + "'");

                if (returnedSyndFeed == null) {
                    logger.debug("no feeds updated, return null!");
                    return null;
                }
            }
        } catch (Exception e) {
        	throw new MessagingException("Exception thrown when trying to retrive feed at url '" + this.feedURLObject + "'", e);
        }

        return returnedSyndFeed;
    }

    public Message<SyndFeed> receive() {
        SyndFeed syndFeed = this.receiveSyndFeed();

        if (null == syndFeed) {
            return null;
        }

        return MessageBuilder.withPayload(syndFeed).setHeader(FeedConstants.FEED_URL, this.feedURLObject).build();
    }

    @Override
    protected void onInit() throws Exception {

//        myFetcherListener = new MyFetcherListener();
        fetcherCache = HashMapFeedInfoCache.getInstance();

        fetcher = new HttpURLFeedFetcher(fetcherCache);

        fetcher.addFetcherEventListener(myFetcherListener);
        Assert.notNull(this.feedUrl, "the feedURL can't be null");
        feedURLObject = new URL(this.feedUrl);
    }
    
    class MyFetcherListener implements FetcherListener {
        /**
         * @see com.sun.syndication.fetcher.FetcherListener#fetcherEvent(com.sun.syndication.fetcher.FetcherEvent)
         */
        public void fetcherEvent(final FetcherEvent event) {
            String eventType = event.getEventType();

            if (FetcherEvent.EVENT_TYPE_FEED_POLLED.equals(eventType)) {
                logger.debug("\tEVENT: Feed Polled. URL = " + event.getUrlString());
            } else if (FetcherEvent.EVENT_TYPE_FEED_RETRIEVED.equals(eventType)) {
                logger.debug("\tEVENT: Feed Retrieved. URL = " + event.getUrlString());
                syndFeeds.add(event.getFeed());
            } else if (FetcherEvent.EVENT_TYPE_FEED_UNCHANGED.equals(eventType)) {
                logger.debug("\tEVENT: Feed Unchanged. URL = " + event.getUrlString());
            }
        }
    }
}