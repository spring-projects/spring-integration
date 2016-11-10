/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.feed.dsl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.util.Properties;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.rometools.rome.feed.synd.SyndEntry;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class FeedDslTests {

	@ClassRule
	public final static TemporaryFolder tempFolder = new TemporaryFolder();

	@Autowired
	private PollableChannel entries;

	@Autowired
	private PropertiesPersistingMetadataStore metadataStore;

	@Test
	@SuppressWarnings("unchecked")
	public void testFeedEntryMessageSourceFlow() throws Exception {
		Message<SyndEntry> message1 = (Message<SyndEntry>) this.entries.receive(10000);
		Message<SyndEntry> message2 = (Message<SyndEntry>) this.entries.receive(10000);
		Message<SyndEntry> message3 = (Message<SyndEntry>) this.entries.receive(10000);
		long time1 = message1.getPayload().getPublishedDate().getTime();
		long time2 = message2.getPayload().getPublishedDate().getTime();
		long time3 = message3.getPayload().getPublishedDate().getTime();
		assertTrue(time1 < time2);
		assertTrue(time2 < time3);
		assertNull(this.entries.receive(10));

		this.metadataStore.flush();

		FileReader metadataStoreFile =
				new FileReader(tempFolder.getRoot().getAbsolutePath() + "/metadata-store.properties");
		Properties metadataStoreProperties = new Properties();
		metadataStoreProperties.load(metadataStoreFile);
		assertFalse(metadataStoreProperties.isEmpty());
		assertEquals(1, metadataStoreProperties.size());
		assertTrue(metadataStoreProperties.containsKey("feedTest"));
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Value("org/springframework/integration/feed/sample.rss")
		private Resource feedResource;

		@Bean
		public MetadataStore metadataStore() {
			PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
			metadataStore.setBaseDirectory(tempFolder.getRoot().getAbsolutePath());
			return metadataStore;
		}

		@Bean
		public IntegrationFlow feedFlow() {
			return IntegrationFlows
					.from(Feed.inboundAdapter(this.feedResource, "feedTest")
									.metadataStore(metadataStore())
									.preserveWireFeed(true),
							e -> e.poller(p -> p.fixedDelay(100)))
					.channel(c -> c.queue("entries"))
					.get();
		}

	}

}
