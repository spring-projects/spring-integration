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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.integration.Message;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * @author Oleg Zhurakousky
 *
 */
public class FeedEntryReaderMessageSourceTests {

	@Test(expected=IllegalArgumentException.class)
	public void testFailureWhenNotInitialized(){
		FeedEntryReaderMessageSource feedEntrySource = new FeedEntryReaderMessageSource(mock(FeedReaderMessageSource.class));
		feedEntrySource.receive();
	}
	
	@Test
	public void testReceieveFeedWithNoEntries(){
		FeedReaderMessageSource feedReaderSource = mock(FeedReaderMessageSource.class);
		SyndFeed feed = mock(SyndFeed.class);
		when(feedReaderSource.receiveSyndFeed()).thenReturn(feed);
		FeedEntryReaderMessageSource feedEntrySource = new FeedEntryReaderMessageSource(feedReaderSource);
		feedEntrySource.afterPropertiesSet();
		assertNull(feedEntrySource.receive());
	}
	@Test
	public void testReceieveFeedWithEntriesSorted(){
		FeedReaderMessageSource feedReaderSource = mock(FeedReaderMessageSource.class);
		SyndFeed feed = mock(SyndFeed.class);
		SyndEntry entry1 = mock(SyndEntry.class);
		SyndEntry entry2 = mock(SyndEntry.class);
		when(entry1.getPublishedDate()).thenReturn(new Date(System.currentTimeMillis()));
		when(entry2.getPublishedDate()).thenReturn(new Date(System.currentTimeMillis()-10000));
		
		List<SyndEntry> entries = new ArrayList<SyndEntry>();
		entries.add(entry2);
		entries.add(entry1);
		when(feed.getEntries()).thenReturn(entries);
		when(feedReaderSource.receiveSyndFeed()).thenReturn(feed);
		
		FeedEntryReaderMessageSource feedEntrySource = new FeedEntryReaderMessageSource(feedReaderSource);
		feedEntrySource.afterPropertiesSet();
		Message<SyndEntry> entryMessage = feedEntrySource.receive();
		assertEquals(entry2, entryMessage.getPayload());
		entryMessage = feedEntrySource.receive();
		assertEquals(entry1, entryMessage.getPayload());
		reset(feed);
		entryMessage = feedEntrySource.receive();
		assertNull(entryMessage);
	}
}
