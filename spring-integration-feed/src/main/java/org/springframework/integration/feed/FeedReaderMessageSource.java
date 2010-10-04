package org.springframework.integration.feed;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherListener;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HashMapFeedInfoCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.context.metadata.MetadataPersister;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * The idea behind this class is that {@link org.springframework.integration.core.MessageSource#receive()} will only
 * return a {@link SyndFeed} when the event listener tells us that a feed has been updated. If we can ascertain that
 * it's been updated, then we can add the item to the {@link java.util.Queue} implementation.
 *
 * @author Josh Long
 * @author Mario Gray
 */
public class FeedReaderMessageSource extends IntegrationObjectSupport
        implements InitializingBean, Lifecycle, MessageSource<SyndFeed> {
    private volatile boolean running;
    private volatile String feedUrl;
    private volatile URL feedURLObject;
    private volatile FeedFetcherCache fetcherCache;
    private volatile HttpURLFeedFetcher fetcher;
    private volatile ConcurrentLinkedQueue<SyndFeed> syndFeeds;
    private volatile MyFetcherListener myFetcherListener;

    public FeedReaderMessageSource() {
        syndFeeds = new ConcurrentLinkedQueue<SyndFeed>();
    }

    private volatile MetadataPersister persister;

    @Override
    protected void onInit() throws Exception {

        this.persister = this.getRequiredMetadataPersister();

        myFetcherListener = new MyFetcherListener();
        fetcherCache = HashMapFeedInfoCache.getInstance();

        fetcher = new HttpURLFeedFetcher(fetcherCache);

        // fetcher.set
        fetcher.addFetcherEventListener(myFetcherListener);
        Assert.notNull(this.feedUrl, "the feedURL can't be null");
        feedURLObject = new URL(this.feedUrl);
/*
        String id = FeedReaderMessageSource.class.getName() + "#" + feedUrl;

        StringBuffer stringBuffer = new StringBuffer();

        for (char c : id.toCharArray())
            if (Character.isDigit(c) || Character.isLetter(c))
                stringBuffer.append(c);
        id = stringBuffer.toString();

        this.feedMetadataIdKey = id;


        long lastTimeNo = -1;
        String lastTime = (String) this.persister.read(this.feedMetadataIdKey);
        if (lastTime != null && !lastTime.trim().equalsIgnoreCase("")) {
            lastTimeNo = Long.parseLong(lastTime);
            this.lastTime = lastTimeNo;
        }*/

    }

    private volatile long lastTime = -1;

    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }

    private String feedMetadataIdKey;
    private final Object syndFeedMonitor = new Object();

    public SyndFeed receiveSyndFeed() {
        SyndFeed returnedSyndFeed = null;

        try {
            synchronized (syndFeedMonitor) {
                fetcher.retrieveFeed(this.feedURLObject);
                logger.debug("attempted to retrieve feed '" + this.feedUrl + "'");
                returnedSyndFeed = syndFeeds.poll(); // there wont be things whose pub date is < than the lastTime

                if (null == returnedSyndFeed) {
                    logger.debug("no feeds updated, return null!");
                    return null;
                }
                // so its OK to update the lastTime
           //

               /*  this.lastTime = sortId(returnedSyndFeed);if (null != this.persister)
                    this.persister.write(this.feedMetadataIdKey, this.lastTime + "");
*/
            }
        } catch (Throwable e) {
            logger.debug("Exception thrown when trying to retrive feed at url '" + this.feedURLObject + "'", e);
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

    public boolean isRunning() {
        return this.running;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(final String feedUrl) {
        this.feedUrl = feedUrl;
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
              //  if (sortId(event.getFeed()) > lastTime) // its true if the lastTime is -1 || N
                    syndFeeds.add(event.getFeed());
            } else if (FetcherEvent.EVENT_TYPE_FEED_UNCHANGED.equals(eventType)) {
                logger.debug("\tEVENT: Feed Unchanged. URL = " + event.getUrlString());
            }
        }
    }
}