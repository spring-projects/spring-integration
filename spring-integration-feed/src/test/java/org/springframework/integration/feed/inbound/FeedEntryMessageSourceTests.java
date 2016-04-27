/*
 * Copyright 2002-2016 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.messaging.Message;

import com.rometools.rome.feed.synd.SyndEntry;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Aaron Loes
 * @author Artem Bilan
 * @since 2.0
 */
@SuppressWarnings("deprecation")
public class FeedEntryMessageSourceTests {

	private final com.rometools.fetcher.FeedFetcher feedFetcher = new FileUrlFeedFetcher();

	@Before
	public void prepare() {
		File metadataStoreFile = new File(System.getProperty("java.io.tmpdir") + "/spring-integration/",
				"metadata-store.properties");
		if (metadataStoreFile.exists()) {
			metadataStoreFile.delete();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFailureWhenNotInitialized() throws Exception {
		URL url = new ClassPathResource("org/springframework/integration/feed/sample.rss").getURL();
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(url, "foo");
		feedEntrySource.receive();
	}

	@Test
	public void testReceiveFeedWithNoEntries() throws Exception {
		URL url = new ClassPathResource("org/springframework/integration/feed/empty.rss").getURL();
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(url, "foo", this.feedFetcher);
		feedEntrySource.setBeanName("feedReader");
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
		feedEntrySource.afterPropertiesSet();
		assertNull(feedEntrySource.receive());
	}

	@Test
	public void testReceiveFeedWithEntriesSorted() throws Exception {
		URL url = new ClassPathResource("org/springframework/integration/feed/sample.rss").getURL();
		FeedEntryMessageSource source = new FeedEntryMessageSource(url, "foo", this.feedFetcher);
		source.setComponentName("feedReader");
		source.setBeanFactory(mock(BeanFactory.class));
		source.afterPropertiesSet();
		Message<SyndEntry> message1 = source.receive();
		Message<SyndEntry> message2 = source.receive();
		Message<SyndEntry> message3 = source.receive();
		long time1 = message1.getPayload().getPublishedDate().getTime();
		long time2 = message2.getPayload().getPublishedDate().getTime();
		long time3 = message3.getPayload().getPublishedDate().getTime();
		assertTrue(time1 < time2);
		assertTrue(time2 < time3);
		assertNull(source.receive());
	}

	// verifies that when entry has been updated since publish, that is taken into
	// account when determining if the feed entry has been seen before
	@Test
	public void testEntryHavingBeenUpdatedAfterPublishAndRepeat() throws Exception {
		URL url = new ClassPathResource("org/springframework/integration/feed/atom.xml").getURL();
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(url, "foo", this.feedFetcher);
		feedEntrySource.setBeanName("feedReader");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
		feedEntrySource.afterPropertiesSet();

		SyndEntry entry1 = feedEntrySource.receive().getPayload();
		assertNull(feedEntrySource.receive()); // only 1 entries in the test feed

		assertEquals("Atom draft-07 snapshot", entry1.getTitle().trim());
		assertEquals(1071318569000L, entry1.getPublishedDate().getTime());
		assertEquals(1122812969000L, entry1.getUpdatedDate().getTime());

		metadataStore.destroy();
		metadataStore.afterPropertiesSet();

		// now test that what's been read is no longer retrieved
		feedEntrySource = new FeedEntryMessageSource(url, "foo", this.feedFetcher);
		feedEntrySource.setBeanName("feedReader");
		metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
		feedEntrySource.afterPropertiesSet();
		assertNull(feedEntrySource.receive());
	}

	// will test that last feed entry is remembered between the sessions
	// and no duplicate entries are retrieved
	@Test
	public void testReceiveFeedWithRealEntriesAndRepeatWithPersistentMetadataStore() throws Exception {
		URL url = new ClassPathResource("org/springframework/integration/feed/sample.rss").getURL();
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(url, "foo", this.feedFetcher);
		feedEntrySource.setBeanName("feedReader");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
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
		feedEntrySource = new FeedEntryMessageSource(url, "foo", this.feedFetcher);
		feedEntrySource.setBeanName("feedReader");
		metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
		feedEntrySource.afterPropertiesSet();
		assertNull(feedEntrySource.receive());
		assertNull(feedEntrySource.receive());
		assertNull(feedEntrySource.receive());
	}

	// will test that last feed entry is NOT remembered between the sessions, since
	// no persistent MetadataStore is provided and the same entries are retrieved again
	@Test
	public void testReceiveFeedWithRealEntriesAndRepeatNoPersistentMetadataStore() throws Exception {
		URL url = new ClassPathResource("org/springframework/integration/feed/sample.rss").getURL();
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(url, "foo", this.feedFetcher);
		feedEntrySource.setBeanName("feedReader");
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
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
		feedEntrySource = new FeedEntryMessageSource(url, "foo", this.feedFetcher);
		feedEntrySource.setBeanName("feedReader");
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
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
