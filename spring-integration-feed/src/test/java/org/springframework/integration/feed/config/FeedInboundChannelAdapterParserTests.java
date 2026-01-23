/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.feed.config;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.SyndFeedInput;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.feed.inbound.FeedEntryMessageSource;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.0
 */
public class FeedInboundChannelAdapterParserTests {

	@TempDir
	public static File tempFolder;

	private static CountDownLatch latch;

	@Test
	public void validateSuccessfulFileConfigurationWithCustomMetadataStore() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"FeedInboundChannelAdapterParserTests-file-context.xml", this.getClass());
		SourcePollingChannelAdapter adapter = context.getBean("feedAdapter", SourcePollingChannelAdapter.class);
		FeedEntryMessageSource source = TestUtils.getPropertyValue(adapter, "source");
		assertThat(TestUtils.<String>getPropertyValue(source, "metadataKey"))
				.isEqualTo("feedAdapter");
		assertThat(TestUtils.<Object>getPropertyValue(source, "metadataStore"))
				.isSameAs(context.getBean(MetadataStore.class));
		SyndFeedInput syndFeedInput = TestUtils.getPropertyValue(source, "syndFeedInput");
		assertThat(syndFeedInput).isSameAs(context.getBean(SyndFeedInput.class));
		assertThat(syndFeedInput.isPreserveWireFeed()).isFalse();
		context.close();
	}

	@Test
	public void validateSuccessfulHttpConfigurationWithCustomMetadataStore() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"FeedInboundChannelAdapterParserTests-http-context.xml", this.getClass());
		SourcePollingChannelAdapter adapter = context.getBean("feedAdapter", SourcePollingChannelAdapter.class);
		FeedEntryMessageSource source = (FeedEntryMessageSource) TestUtils.getPropertyValue(adapter, "source");
		assertThat(TestUtils.<Object>getPropertyValue(source, "metadataStore")).isNotNull();
		context.close();
	}

	@Test
	public void validateSuccessfulNewsRetrievalWithFileUrlAndMessageHistory() throws Exception {
		//Test file samples.rss has 3 news items
		latch = new CountDownLatch(3);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"FeedInboundChannelAdapterParserTests-file-usage-context.xml", this.getClass());
		latch.await(10, TimeUnit.SECONDS);
		assertThat(latch.getCount()).isEqualTo(0);
		context.close();

		// since we are not deleting the persister file
		// in this iteration no new feeds will be received and the latch will timeout
		latch = new CountDownLatch(3);
		context = new ClassPathXmlApplicationContext(
				"FeedInboundChannelAdapterParserTests-file-usage-context.xml", this.getClass());
		latch.await(500, TimeUnit.MILLISECONDS);
		assertThat(latch.getCount()).isEqualTo(3);

		SourcePollingChannelAdapter adapter = context.getBean("feedAdapterUsage", SourcePollingChannelAdapter.class);
		assertThat(TestUtils.<Boolean>getPropertyValue(adapter, "source.syndFeedInput.preserveWireFeed"))
				.isTrue();

		context.close();
	}

	@Test
	@Disabled("Goes against the real feed")
	public void validateSuccessfulNewsRetrievalWithHttpUrl() throws Exception {
		final CountDownLatch latch = new CountDownLatch(3);
		MessageHandler handler = spy(message -> latch.countDown());
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"FeedInboundChannelAdapterParserTests-http-context.xml", this.getClass());
		DirectChannel feedChannel = context.getBean("feedChannel", DirectChannel.class);
		feedChannel.subscribe(handler);
		latch.await(10, TimeUnit.SECONDS);
		verify(handler, atLeast(3)).handleMessage(Mockito.any(Message.class));
		context.close();
	}

	@Test
	public void testAutoChannel() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"FeedInboundChannelAdapterParserTests-autoChannel-context.xml", this.getClass());
		MessageChannel autoChannel = context.getBean("autoChannel", MessageChannel.class);
		SourcePollingChannelAdapter adapter = context.getBean("autoChannel.adapter",
				SourcePollingChannelAdapter.class);
		assertThat(TestUtils.<Object>getPropertyValue(adapter, "outputChannel")).isSameAs(autoChannel);
		context.close();
	}

	public static class SampleService {

		public void receiveFeedEntry(Message<?> message) {
			MessageHistory history = MessageHistory.read(message);
			assertThat(history).hasSize(3);
			Properties historyItem = history.get(0);
			assertThat(historyItem)
					.containsEntry("name", "feedAdapterUsage")
					.containsEntry("type", "feed:inbound-channel-adapter");

			historyItem = history.get(1);
			assertThat(historyItem)
					.containsEntry("name", "feedChannelUsage")
					.containsEntry("type", "channel");

			historyItem = history.get(2);
			assertThat(historyItem)
					.containsEntry("name", "sampleActivator")
					.containsEntry("type", "service-activator");
			latch.countDown();
		}

	}

	public static class SampleServiceNoHistory {

		public void receiveFeedEntry(SyndEntry entry) {
			latch.countDown();
		}

	}

}
