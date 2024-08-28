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

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.rometools.rome.feed.synd.SyndEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Aaron Loes
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class FeedEntryMessageSourceTests {

	static File metadataStoreFile;

	@BeforeEach
	public void prepare() {
		metadataStoreFile = new File(System.getProperty("java.io.tmpdir") + "/spring-integration/",
				"metadata-store.properties");
		if (metadataStoreFile.exists()) {
			metadataStoreFile.delete();
		}
	}

	@AfterAll
	static void tearDown() {
		if (metadataStoreFile.exists()) {
			metadataStoreFile.delete();
		}
	}

	@Test
	public void testFailureWhenNotInitialized() throws Exception {
		URL url = new ClassPathResource("org/springframework/integration/feed/sample.rss").getURL();
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(url, "foo");
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(feedEntrySource::receive);
	}

	@Test
	public void testReceiveFeedWithNoEntries() throws Exception {
		URL url = new ClassPathResource("org/springframework/integration/feed/empty.rss").getURL();
		UrlResource urlResource =
				new UrlResource(url) {

					@Override
					protected void customizeConnection(HttpURLConnection connection) throws IOException {
						super.customizeConnection(connection);
						connection.setConnectTimeout(10000);
						connection.setReadTimeout(5000);
					}
				};
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(urlResource, "foo");
		feedEntrySource.setBeanName("feedReader");
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
		feedEntrySource.afterPropertiesSet();
		assertThat(feedEntrySource.receive()).isNull();
	}

	@Test
	public void testReceiveFeedWithEntriesSorted() {
		ClassPathResource resource = new ClassPathResource("org/springframework/integration/feed/sample.rss");
		FeedEntryMessageSource source = new FeedEntryMessageSource(resource, "foo");
		source.setBeanName("feedReader");
		source.setBeanFactory(mock(BeanFactory.class));
		source.afterPropertiesSet();
		Message<SyndEntry> message1 = source.receive();
		Message<SyndEntry> message2 = source.receive();
		Message<SyndEntry> message3 = source.receive();
		long time1 = message1.getPayload().getPublishedDate().getTime();
		long time2 = message2.getPayload().getPublishedDate().getTime();
		long time3 = message3.getPayload().getPublishedDate().getTime();
		assertThat(time1 < time2).isTrue();
		assertThat(time2 < time3).isTrue();
		assertThat(source.receive()).isNull();
	}

	// verifies that when entry has been updated since publish, that is taken into
	// account when determining if the feed entry has been seen before
	@Test
	public void testEntryHavingBeenUpdatedAfterPublishAndRepeat() {
		ClassPathResource resource = new ClassPathResource("org/springframework/integration/feed/atom.xml");
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(resource, "foo");
		feedEntrySource.setBeanName("feedReader");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
		feedEntrySource.afterPropertiesSet();

		SyndEntry entry1 = feedEntrySource.receive().getPayload();
		assertThat(feedEntrySource.receive()).isNull(); // only 1 entries in the test feed

		assertThat(entry1.getTitle().trim()).isEqualTo("Atom draft-07 snapshot");
		assertThat(entry1.getPublishedDate().getTime()).isEqualTo(1071318569000L);
		assertThat(entry1.getUpdatedDate().getTime()).isEqualTo(1122812969000L);

		metadataStore.destroy();
		metadataStore.afterPropertiesSet();

		// now test that what's been read is no longer retrieved
		feedEntrySource = new FeedEntryMessageSource(resource, "foo");
		feedEntrySource.setBeanName("feedReader");
		metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
		feedEntrySource.afterPropertiesSet();
		assertThat(feedEntrySource.receive()).isNull();
	}

	// will test that last feed entry is remembered between the sessions
	// and no duplicate entries are retrieved
	@Test
	public void testReceiveFeedWithRealEntriesAndRepeatWithPersistentMetadataStore() {
		ClassPathResource resource = new ClassPathResource("org/springframework/integration/feed/sample.rss");
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(resource, "foo");
		feedEntrySource.setBeanName("feedReader");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
		feedEntrySource.afterPropertiesSet();
		SyndEntry entry1 = feedEntrySource.receive().getPayload();
		SyndEntry entry2 = feedEntrySource.receive().getPayload();
		SyndEntry entry3 = feedEntrySource.receive().getPayload();
		assertThat(feedEntrySource.receive()).isNull(); // only 3 entries in the test feed

		assertThat(entry1.getTitle().trim()).isEqualTo("Spring Integration download");
		assertThat(entry1.getPublishedDate().getTime()).isEqualTo(1266088337000L);

		assertThat(entry2.getTitle().trim()).isEqualTo("Check out Spring Integration forums");
		assertThat(entry2.getPublishedDate().getTime()).isEqualTo(1268469501000L);

		assertThat(entry3.getTitle().trim()).isEqualTo("Spring Integration adapters");
		assertThat(entry3.getPublishedDate().getTime()).isEqualTo(1272044098000L);

		metadataStore.destroy();
		metadataStore.afterPropertiesSet();

		// now test that what's been read is no longer retrieved
		feedEntrySource = new FeedEntryMessageSource(resource, "foo");
		feedEntrySource.setBeanName("feedReader");
		metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
		feedEntrySource.afterPropertiesSet();
		assertThat(feedEntrySource.receive()).isNull();
		assertThat(feedEntrySource.receive()).isNull();
		assertThat(feedEntrySource.receive()).isNull();
	}

	// will test that last feed entry is NOT remembered between the sessions, since
	// no persistent MetadataStore is provided and the same entries are retrieved again
	@Test
	public void testReceiveFeedWithRealEntriesAndRepeatNoPersistentMetadataStore() {
		ClassPathResource resource = new ClassPathResource("org/springframework/integration/feed/sample.rss");
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(resource, "foo");
		feedEntrySource.setBeanName("feedReader");
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
		feedEntrySource.afterPropertiesSet();
		SyndEntry entry1 = feedEntrySource.receive().getPayload();
		SyndEntry entry2 = feedEntrySource.receive().getPayload();
		SyndEntry entry3 = feedEntrySource.receive().getPayload();
		assertThat(feedEntrySource.receive()).isNull(); // only 3 entries in the test feed

		assertThat(entry1.getTitle().trim()).isEqualTo("Spring Integration download");
		assertThat(entry1.getPublishedDate().getTime()).isEqualTo(1266088337000L);

		assertThat(entry2.getTitle().trim()).isEqualTo("Check out Spring Integration forums");
		assertThat(entry2.getPublishedDate().getTime()).isEqualTo(1268469501000L);

		assertThat(entry3.getTitle().trim()).isEqualTo("Spring Integration adapters");
		assertThat(entry3.getPublishedDate().getTime()).isEqualTo(1272044098000L);

		// UNLIKE the previous test
		// now test that what's been read is read AGAIN
		feedEntrySource = new FeedEntryMessageSource(resource, "foo");
		feedEntrySource.setBeanName("feedReader");
		feedEntrySource.setBeanFactory(mock(BeanFactory.class));
		feedEntrySource.afterPropertiesSet();
		entry1 = feedEntrySource.receive().getPayload();
		entry2 = feedEntrySource.receive().getPayload();
		entry3 = feedEntrySource.receive().getPayload();
		assertThat(feedEntrySource.receive()).isNull(); // only 3 entries in the test feed

		assertThat(entry1.getTitle().trim()).isEqualTo("Spring Integration download");
		assertThat(entry1.getPublishedDate().getTime()).isEqualTo(1266088337000L);

		assertThat(entry2.getTitle().trim()).isEqualTo("Check out Spring Integration forums");
		assertThat(entry2.getPublishedDate().getTime()).isEqualTo(1268469501000L);

		assertThat(entry3.getTitle().trim()).isEqualTo("Spring Integration adapters");
		assertThat(entry3.getPublishedDate().getTime()).isEqualTo(1272044098000L);
	}

}
