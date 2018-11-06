/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.metadata;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.Phaser;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 **/
@RunWith(SpringRunner.class)
public abstract class MetadataStoreReaperTests {

	private static Duration timeToLive = Duration.ofSeconds(60);

	protected void verifyMetadataStore(Phaser phaser, MetadataStore metadataStore) {
		//Wait for metadataStore initialization to complete and reaper is running.
		phaser.arriveAndAwaitAdvance();
		//Wait for reaper to complete.
		phaser.arriveAndAwaitAdvance();

		assertThat(metadataStore.keySet()).hasSize(6);
		assertThat(metadataStore.keySet()).containsExactlyInAnyOrder("key0", "key1", "key2", "key3", "key4",
				"key5");
		assertThat(metadataStore.get("key6")).isNull();
		assertThat(metadataStore.get("key7")).isNull();
		assertThat(metadataStore.get("key8")).isNull();
		assertThat(metadataStore.get("key9")).isNull();
	}

	@ContextConfiguration(classes = { MetadataStoreReaperTests.DefaultMetadataStoreReaperConfig.class,
			MetadataStoreReaperTests.MetadataStoreReaperConfig.class })
	public static class DurationBasedExpirationTests extends MetadataStoreReaperTests {

		@Autowired
		private MetadataStore metadataStore;

		@Autowired
		private Phaser phaser;

		@Test
		public void defaultDurationBasedExpiration() {
			ZonedDateTime now = ZonedDateTime.now();
			for (int i = 0; i < 10; i++) {
				metadataStore.put("key" + i, String.valueOf(now.minusSeconds(i * 10).toInstant().toEpochMilli()));
			}

			verifyMetadataStore(phaser, metadataStore);
		}
	}

	@ContextConfiguration(classes = { MetadataStoreReaperTests.CustomMetadataStoreReaperConfig.class,
			MetadataStoreReaperTests.MetadataStoreReaperConfig.class })
	public static class CustomExpirationTests extends MetadataStoreReaperTests {

		@Autowired
		private MetadataStore metadataStore;

		@Autowired
		private Phaser phaser;

		@Test
		public void customExpiration() {
			ZonedDateTime now = ZonedDateTime.now();
			for (int i = 0; i < 10; i++) {
				metadataStore.put("key" + i, String.valueOf(now.minusSeconds(i * 10)));
			}

			verifyMetadataStore(phaser, metadataStore);
		}
	}

	@Configuration
	static class DefaultMetadataStoreReaperConfig {

		@Bean
		MetadataStoreReaper metadataStoreReaper(MetadataStore metadataStore) {
			return new MetadataStoreReaper(metadataStore, timeToLive);
		}
	}

	@Configuration
	static class CustomMetadataStoreReaperConfig {

		@Bean
		MetadataStoreReaper metadataStoreReaper(MetadataStore metadataStore) {
			return new MetadataStoreReaper(metadataStore, v -> {
				ZonedDateTime now = ZonedDateTime.now();
				ZonedDateTime dateTime = ZonedDateTime.parse(v);
				return Duration.between(dateTime, now).compareTo(timeToLive) >= 0;
			});
		}
	}

	@Configuration
	@EnableScheduling
	static class MetadataStoreReaperConfig {

		@Autowired
		private MetadataStoreReaper metadataStoreReaper;

		private Phaser phaser = new Phaser(2);

		@Bean
		MetadataStore metadataStore() {
			return new SimpleMetadataStore();
		}

		@Bean
		Phaser phaser() {
			return phaser;
		}

		@Scheduled(fixedRate = 10000)
		public void reapMetadataStore() {
			phaser.arriveAndAwaitAdvance();
			metadataStoreReaper.run();
			phaser.arriveAndAwaitAdvance();
		}

	}

}


