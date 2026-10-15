/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.feed.inbound;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

import com.rometools.rome.feed.synd.SyndEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Aaron Loes
 * @author Artem Bilan
 * @author Jialin Chen
 *
 * @since 2.0
 */
public class FeedEntryMessageSourceTests implements TestApplicationContextAware {

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
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(url, "key");
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
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(urlResource, "key");
		feedEntrySource.setBeanName("feedReader");
		feedEntrySource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		feedEntrySource.afterPropertiesSet();
		assertThat(feedEntrySource.receive()).isNull();
	}

	@Test
	public void testReceiveFeedWithEntriesSorted() {
		ClassPathResource resource = new ClassPathResource("org/springframework/integration/feed/sample.rss");
		FeedEntryMessageSource source = new FeedEntryMessageSource(resource, "key");
		source.setBeanName("feedReader");
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
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

	@Test
	public void testReceiveFeedWithDatelessEntriesSortedLast() {
		ClassPathResource resource = new ClassPathResource("org/springframework/integration/feed/mixed-dates.rss");
		FeedEntryMessageSource source = new FeedEntryMessageSource(resource, "mixedDates");
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		source.afterPropertiesSet();

		assertThat(source.receive()).extracting(message -> message.getPayload().getTitle()).isEqualTo("Dated entry");
		assertThat(source.receive()).extracting(message -> message.getPayload().getTitle())
				.isEqualTo("First undated entry");
		assertThat(source.receive()).extracting(message -> message.getPayload().getTitle())
				.isEqualTo("Second undated entry");
		assertThat(source.receive()).isNull();
	}

	@Test
	public void testReceiveFeedWithoutEntryDatesUsesFeedDate(@TempDir Path directory) throws IOException {
		ClassPathResource resource = new ClassPathResource("org/springframework/integration/feed/dateless.rss");
		String feedContent = resource.getContentAsString(StandardCharsets.UTF_8);
		Path feedFile = directory.resolve("feed.rss");
		Files.writeString(feedFile, feedContent);
		SimpleMetadataStore metadataStore = new SimpleMetadataStore();
		FeedEntryMessageSource source = new FeedEntryMessageSource(new FileSystemResource(feedFile), "datelessEntries");
		source.setMetadataStore(metadataStore);
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		source.afterPropertiesSet();

		Instant feedDate = Instant.parse("2026-10-07T10:00:00Z");
		for (int i = 0; i < 3; i++) {
			Message<SyndEntry> message = source.receive();
			assertThat(message).isNotNull();
			assertThat(message.getPayload().getUpdatedDate()).isEqualTo(Date.from(feedDate));
		}
		assertThat(source.receive()).isNull();
		assertThat(metadataStore.get("datelessEntries")).isEqualTo(Long.toString(feedDate.toEpochMilli()));

		source = new FeedEntryMessageSource(new FileSystemResource(feedFile), "datelessEntries");
		source.setMetadataStore(metadataStore);
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		source.afterPropertiesSet();
		assertThat(source.receive()).isNull();

		Files.writeString(feedFile,
				feedContent.replace("Wed, 07 Oct 2026 10:00:00 GMT", "Thu, 08 Oct 2026 10:00:00 GMT"));
		for (int i = 0; i < 3; i++) {
			assertThat(source.receive()).isNotNull();
		}
		assertThat(source.receive()).isNull();
	}

	@Test
	public void testReceiveFeedWithoutAnyDates() throws IOException {
		ClassPathResource resource = new ClassPathResource("org/springframework/integration/feed/dateless.rss");
		String feedContent = resource.getContentAsString(StandardCharsets.UTF_8)
				.replace("<pubDate>Wed, 07 Oct 2026 10:00:00 GMT</pubDate>", "");
		FeedEntryMessageSource source =
				new FeedEntryMessageSource(new ByteArrayResource(feedContent.getBytes()), "noDates");
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		source.afterPropertiesSet();

		assertThat(source.receive()).isNull();
	}

	// verifies that when entry has been updated since publication that is taken into
	// account when determining if the feed entry has been seen before
	@Test
	public void testEntryHavingBeenUpdatedAfterPublishAndRepeat() {
		ClassPathResource resource = new ClassPathResource("org/springframework/integration/feed/atom.xml");
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(resource, "key");
		feedEntrySource.setBeanName("feedReader");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		feedEntrySource.afterPropertiesSet();

		SyndEntry entry1 = feedEntrySource.receive().getPayload();
		assertThat(feedEntrySource.receive()).isNull(); // only 1 entry in the test feed

		assertThat(entry1.getTitle().trim()).isEqualTo("Atom draft-07 snapshot");
		assertThat(entry1.getPublishedDate().getTime()).isEqualTo(1071318569000L);
		assertThat(entry1.getUpdatedDate().getTime()).isEqualTo(1122812969000L);

		metadataStore.destroy();
		metadataStore.afterPropertiesSet();

		// now test that what's been read is no longer retrieved
		feedEntrySource = new FeedEntryMessageSource(resource, "key");
		feedEntrySource.setBeanName("feedReader");
		metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		feedEntrySource.afterPropertiesSet();
		assertThat(feedEntrySource.receive()).isNull();
	}

	// will test that last feed entry is remembered between the sessions
	// and no duplicate entries are retrieved
	@Test
	public void testReceiveFeedWithRealEntriesAndRepeatWithPersistentMetadataStore() {
		ClassPathResource resource = new ClassPathResource("org/springframework/integration/feed/sample.rss");
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(resource, "key");
		feedEntrySource.setBeanName("feedReader");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
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
		feedEntrySource = new FeedEntryMessageSource(resource, "key");
		feedEntrySource.setBeanName("feedReader");
		metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		feedEntrySource.setMetadataStore(metadataStore);
		feedEntrySource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
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
		FeedEntryMessageSource feedEntrySource = new FeedEntryMessageSource(resource, "key");
		feedEntrySource.setBeanName("feedReader");
		feedEntrySource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
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
		feedEntrySource = new FeedEntryMessageSource(resource, "key");
		feedEntrySource.setBeanName("feedReader");
		feedEntrySource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
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
