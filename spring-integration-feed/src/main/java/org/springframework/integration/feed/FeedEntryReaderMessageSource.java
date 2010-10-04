package org.springframework.integration.feed;


import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import org.springframework.context.Lifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.context.metadata.MetadataPersister;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * this is a slightly different use case than {@link org.springframework.integration.feed.FeedReaderMessageSource}.
 * This returns which entries are added, which is a more nuanced use case requiring some of our own caching.
 * <em>NB:</em> this does <strong>not</strong> somehow detect entry removal from a feed.
 *
 * @author Josh Long
 * @author Mario Gray
 */
public class FeedEntryReaderMessageSource extends IntegrationObjectSupport implements MessageSource<SyndEntry>, Lifecycle {

    private volatile ConcurrentLinkedQueue<SyndEntry> entries;
    private volatile MetadataPersister persister;
    private volatile FeedReaderMessageSource feedReaderMessageSource;
    private final Object monitor = new Object();
    private String feedMetadataIdKey;
    private String feedUrl;
    private volatile boolean running;

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
    //   private Queue<SyndEntry> entries;
    private volatile long lastTime = -1;

    public FeedEntryReaderMessageSource() {
        //     this.entries = new ConcurrentSkipListSet<SyndEntry>(new MyComparator());
        this.entries = new ConcurrentLinkedQueue<SyndEntry>();
    }

    public void start() {
        this.feedReaderMessageSource.start();
        this.setRunning(true);

    }


    private long sortId(SyndEntry entry) {
        return entry.getPublishedDate().getTime();
    }


    @Override
    protected void onInit() throws Exception {

        this.persister = this.getRequiredMetadataPersister();

        Assert.notNull(this.feedUrl, "the feedUrl can't be null");
        this.feedReaderMessageSource = new FeedReaderMessageSource();
        this.feedReaderMessageSource.setFeedUrl(this.feedUrl);
        this.feedReaderMessageSource.setBeanFactory(this.getBeanFactory());
        this.feedReaderMessageSource.setBeanName(this.getComponentName());
        this.feedReaderMessageSource.afterPropertiesSet();

        // setup persistence of metadata
        this.feedMetadataIdKey = FeedEntryReaderMessageSource.class.getName() + "#" + feedUrl;
        String lastTime = (String) this.persister.read(this.feedMetadataIdKey);
        if (lastTime != null && !lastTime.trim().equalsIgnoreCase("")) {
            this.lastTime = Long.parseLong(lastTime);
        }
    }

    public void stop() {
        this.feedReaderMessageSource.stop();
        this.setRunning(false);
    }


    public Message<SyndEntry> receive() {
        SyndEntry se = receiveSyndEntry();
        if (se == null) {
            return null;
        }
        return MessageBuilder.withPayload(se).build();
    }

    int longToCompare(long l) {
        if (l < -1) return -1;
        if (l > 1) return 1;
        return 0;
    }

    private Comparator<SyndEntry> syndEntryComparator = new Comparator<SyndEntry>() {
        public int compare(SyndEntry syndEntry, SyndEntry syndEntry1) {
            long x = sortId(syndEntry) - sortId(syndEntry1);
            return longToCompare(x);
        }
    };

    @SuppressWarnings("unchecked")
    public SyndEntry receiveSyndEntry() {
        synchronized (this.monitor) {  // priority goes to the backlog
            SyndEntry nextUp = pollAndCache();

            if (nextUp != null) {
                return nextUp;
            }

            // otherwise, fill the backlog up
            SyndFeed syndFeed = this.feedReaderMessageSource.receiveSyndFeed();
            if (syndFeed != null) {
                List<SyndEntry> feedEntries = (List<SyndEntry>) syndFeed.getEntries();
                if (null != feedEntries) {
                    Collections.sort(feedEntries, syndEntryComparator);
                    for (SyndEntry se : feedEntries) {
                        System.out.println("se: " + se.getPublishedDate().getTime());
                        long sort = this.sortId(se);
                        if (sort > this.lastTime)
                            entries.add(se);
                    }
                }
            }

            return pollAndCache();
        }
    }


    private SyndEntry pollAndCache() {
        SyndEntry next = this.entries.poll();
        if (null == next) return null;
        this.lastTime = sortId(next);
        this.persister.write(this.feedMetadataIdKey, this.lastTime + "");
        return next;
    }


    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(final String feedUrl) {
        this.feedUrl = feedUrl;
    }


    class MyComparator implements Comparator<SyndEntry> {
        public int compare(final SyndEntry syndEntry, final SyndEntry syndEntry1) {
            long val = sortId(syndEntry) - sortId(syndEntry1);
            if (val > 0) return 1;
            if (val < 0) return -1;
            return 0;
        }
    }
}