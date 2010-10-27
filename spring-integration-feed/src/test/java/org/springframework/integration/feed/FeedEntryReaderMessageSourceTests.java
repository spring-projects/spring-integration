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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.context.metadata.PropertiesPersistingMetadataStore;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public class FeedEntryReaderMessageSourceTests {

	@Before
	public void prepare() {
		File metadataStoreFile = new File(System.getProperty("java.io.tmpdir") + "/spring-integration/", "metadata-store.properties");
		if (metadataStoreFile.exists()) {
			metadataStoreFile.delete();
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testFailureWhenNotInitialized() {
		FeedEntryReaderMessageSource feedEntrySource = new FeedEntryReaderMessageSource(mock(FeedReaderMessageSource.class));
		feedEntrySource.receive();
	}

	@Test
	public void testReceiveFeedWithNoEntries() {
		FeedReaderMessageSource feedReaderSource = mock(FeedReaderMessageSource.class);
		SyndFeed feed = mock(SyndFeed.class);
		when(feedReaderSource.receiveFeed()).thenReturn(feed);
		FeedEntryReaderMessageSource feedEntrySource = new FeedEntryReaderMessageSource(feedReaderSource);
		feedEntrySource.setBeanName("feedReader");
		feedEntrySource.afterPropertiesSet();
		assertNull(feedEntrySource.receive());
	}

	@Test
	public void testReceiveFeedWithEntriesSorted() {
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
		when(feedReaderSource.receiveFeed()).thenReturn(feed);
		
		FeedEntryReaderMessageSource feedEntrySource = new FeedEntryReaderMessageSource(feedReaderSource);
		feedEntrySource.setComponentName("feedReader");
		feedEntrySource.afterPropertiesSet();
		Message<SyndEntry> entryMessage = feedEntrySource.receive();
		assertEquals(entry2, entryMessage.getPayload());
		entryMessage = feedEntrySource.receive();
		assertEquals(entry1, entryMessage.getPayload());
		reset(feed);
		entryMessage = feedEntrySource.receive();
		assertNull(entryMessage);
	}

	// will test that last feed entry is remembered between the sessions
	// and no duplicate entries are retrieved
	@Test
	public void testReceiveFeedWithRealEntriesAndRepeatWithPersistentMetadataStore() throws Exception {
		URL url = new URL("file:src/test/java/org/springframework/integration/feed/sample.rss");
		FeedReaderMessageSource feedReaderSource = new FeedReaderMessageSource(url);
		FeedEntryReaderMessageSource feedEntrySource = new FeedEntryReaderMessageSource(feedReaderSource);
		feedEntrySource.setBeanName("feedReader");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.afterPropertiesSet();
		SyndEntry entry1 = feedEntrySource.receive().getPayload();
		SyndEntry entry2 = feedEntrySource.receive().getPayload();
		SyndEntry entry3 = feedEntrySource.receive().getPayload();
		assertNull(feedEntrySource.receive()); // only 3 entries in the test feed
		
		assertEquals("Spring Integration download", entry1.getTitle().trim());
		assertEquals(1266088337000L, entry1.getPublishedDate().getTime());
		
		assertEquals("Check out Spring Integration forums", entry2.getTitle().trim());
		assertEquals(1268469501000L, entry2.getPublishedDate().getTime());
		
		assertEquals("Spring Integration adapters", entry3.getTitle().trim());
		assertEquals(1272044098000L, entry3.getPublishedDate().getTime());
		
		metadataStore.destroy();
		metadataStore.afterPropertiesSet();

		// now test that what's been read is no longer retrieved
		feedEntrySource = new FeedEntryReaderMessageSource(feedReaderSource);
		feedEntrySource.setBeanName("feedReader");
		metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.afterPropertiesSet();
		assertNull(feedEntrySource.receive());
		assertNull(feedEntrySource.receive());
		assertNull(feedEntrySource.receive());
	}

	// will test that last feed entry is NOT remembered between the sessions, since
	// no persistent MetadataStore is provided and the same entries are retrieved again
	@Test
	public void testReceiveFeedWithRealEntriesAndRepeatNoPersistentMetadataStore() throws Exception {
		URL url = new URL("file:src/test/java/org/springframework/integration/feed/sample.rss");
		FeedReaderMessageSource feedReaderSource = new FeedReaderMessageSource(url);
		FeedEntryReaderMessageSource feedEntrySource = new FeedEntryReaderMessageSource(feedReaderSource);
		feedEntrySource.setBeanName("feedReader");
		feedEntrySource.afterPropertiesSet();
		SyndEntry entry1 = feedEntrySource.receive().getPayload();
		SyndEntry entry2 = feedEntrySource.receive().getPayload();
		SyndEntry entry3 = feedEntrySource.receive().getPayload();
		assertNull(feedEntrySource.receive()); // only 3 entries in the test feed
		
		assertEquals("Spring Integration download", entry1.getTitle().trim());
		assertEquals(1266088337000L, entry1.getPublishedDate().getTime());
		
		assertEquals("Check out Spring Integration forums", entry2.getTitle().trim());
		assertEquals(1268469501000L, entry2.getPublishedDate().getTime());
		
		assertEquals("Spring Integration adapters", entry3.getTitle().trim());
		assertEquals(1272044098000L, entry3.getPublishedDate().getTime());
		
		// UNLIKE the previous test
		// now test that what's been read is read AGAIN
		feedEntrySource = new FeedEntryReaderMessageSource(feedReaderSource);
		feedEntrySource.setBeanName("feedReader");
		feedEntrySource.afterPropertiesSet();
		entry1 = feedEntrySource.receive().getPayload();
		entry2 = feedEntrySource.receive().getPayload();
		entry3 = feedEntrySource.receive().getPayload();
		assertNull(feedEntrySource.receive()); // only 3 entries in the test feed
		
		assertEquals("Spring Integration download", entry1.getTitle().trim());
		assertEquals(1266088337000L, entry1.getPublishedDate().getTime());
		
		assertEquals("Check out Spring Integration forums", entry2.getTitle().trim());
		assertEquals(1268469501000L, entry2.getPublishedDate().getTime());
		
		assertEquals("Spring Integration adapters", entry3.getTitle().trim());
		assertEquals(1272044098000L, entry3.getPublishedDate().getTime());
	}

}
