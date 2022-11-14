/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.integration.feed.dsl;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import com.rometools.rome.feed.synd.SyndEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class FeedDslTests {

	@TempDir
	public static File tempFolder;

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
		assertThat(message1).isNotNull();
		assertThat(message2).isNotNull();
		assertThat(message3).isNotNull();
		long time1 = message1.getPayload().getPublishedDate().getTime();
		long time2 = message2.getPayload().getPublishedDate().getTime();
		long time3 = message3.getPayload().getPublishedDate().getTime();
		assertThat(time1 < time2).isTrue();
		assertThat(time2 < time3).isTrue();
		assertThat(this.entries.receive(10)).isNull();

		this.metadataStore.flush();

		FileReader metadataStoreFile =
				new FileReader(tempFolder.getAbsolutePath() + "/metadata-store.properties");
		Properties metadataStoreProperties = new Properties();
		metadataStoreProperties.load(metadataStoreFile);
		assertThat(metadataStoreProperties.isEmpty()).isFalse();
		assertThat(metadataStoreProperties.size()).isEqualTo(1);
		assertThat(metadataStoreProperties.containsKey("feedTest")).isTrue();

		metadataStoreFile.close();
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Value("org/springframework/integration/feed/sample.rss")
		private Resource feedResource;

		@Bean
		public MetadataStore metadataStore() {
			PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
			metadataStore.setBaseDirectory(tempFolder.getAbsolutePath());
			return metadataStore;
		}

		@Bean
		public IntegrationFlow feedFlow() {
			return IntegrationFlow
					.from(Feed.inboundAdapter(this.feedResource, "feedTest")
									.metadataStore(metadataStore())
									.preserveWireFeed(true),
							e -> e.poller(p -> p.fixedDelay(100)))
					.channel(c -> c.queue("entries"))
					.get();
		}

	}

}
