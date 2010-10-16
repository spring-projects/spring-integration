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
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.context.Lifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * This implementation of {@link MessageSource} will produce individual {@link SyndEntry}s for a feed identified 
 * with 'feedUrl' attribute.
 * 
 * @author Josh Long
 * @author Mario Gray
 * @author Oleg Zhurakousky
 */
public class FeedEntryReaderMessageSource extends IntegrationObjectSupport implements MessageSource<SyndEntry>, Lifecycle {

	private volatile Map<String, String> persisterMap = new ConcurrentHashMap<String, String>();
	private volatile Queue<SyndEntry> entries = new ConcurrentLinkedQueue<SyndEntry>();
    private volatile FeedReaderMessageSource feedReaderMessageSource;
    private final Object monitor = new Object();
    private volatile String feedMetadataIdKey;
    private volatile String feedUrl;
    private volatile boolean running;
    private volatile long lastTime = -1;
    
    private Comparator<SyndEntry> syndEntryComparator = new Comparator<SyndEntry>() {
        public int compare(SyndEntry syndEntry, SyndEntry syndEntry1) {
            long x = sortId(syndEntry) - sortId(syndEntry1);
            if (x < -1) {
            	return -1;
            }
            else if (x > 1) {
            	return 1;
            }
            return 0;
        }
    };
    
    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }
    
    public String getFeedUrl() {
        return feedUrl;
    }
    /**
     * Allows you to provide your own implementation of 'persisterMap' instead of relying on
     * your own which is in-memory.
     * 
     * @param persisterMap
     */
    public void setPersisterMap(Map<String, String> persisterMap) {
    	Assert.notNull(persisterMap, "'persisterMap' can not be null");
    	this.persisterMap = persisterMap;
	}
    
    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        this.feedReaderMessageSource.stop();
        this.setRunning(false);
    }
    
    public String getComponentType(){
    	return "feed:inbound-channel-adapter";
    }

    @SuppressWarnings("unchecked")
    public SyndEntry receiveSyndEntry() {
        synchronized (this.monitor) {
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
                        long sort = this.sortId(se);
                        if (sort > this.lastTime)
                            entries.add(se);
                    }
                }
            }
            return pollAndCache();
        }
    }

    public Message<SyndEntry> receive() {
        SyndEntry se = receiveSyndEntry();
        if (se == null) {
            return null;
        }
        return MessageBuilder.withPayload(se).build();
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
        Assert.notNull(this.feedUrl, "the feedUrl can't be null");
        this.feedReaderMessageSource = new FeedReaderMessageSource();
        this.feedReaderMessageSource.setFeedUrl(this.feedUrl);
        this.feedReaderMessageSource.setBeanName(this.getComponentName());
        this.feedReaderMessageSource.afterPropertiesSet();

        // setup persistence of metadata
        this.feedMetadataIdKey = FeedEntryReaderMessageSource.class.getName() + "#" + feedUrl;
        String lastTime = (String) this.persisterMap.get(this.feedMetadataIdKey);
        if (lastTime != null && !lastTime.trim().equalsIgnoreCase("")) {
            this.lastTime = Long.parseLong(lastTime);
        }
    }

    private SyndEntry pollAndCache() {
        SyndEntry next = this.entries.poll();
        
        if (next == null) {
        	return null;
        }
        	
        this.lastTime = sortId(next);
        this.persisterMap.put(this.feedMetadataIdKey, this.lastTime + "");
        return next;
    }
}