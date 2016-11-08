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

package org.springframework.integration.feed.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.SyndFeedInput;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class FeedInboundChannelAdapterParserTests {

	@ClassRule
	public final static TemporaryFolder tempFolder = new TemporaryFolder();


	private static CountDownLatch latch;

	@Test
	public void validateSuccessfulFileConfigurationWithCustomMetadataStore() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"FeedInboundChannelAdapterParserTests-file-context.xml", this.getClass());
		SourcePollingChannelAdapter adapter = context.getBean("feedAdapter", SourcePollingChannelAdapter.class);
		FeedEntryMessageSource source = (FeedEntryMessageSource) TestUtils.getPropertyValue(adapter, "source");
		assertSame(context.getBean(MetadataStore.class), TestUtils.getPropertyValue(source, "metadataStore"));
		SyndFeedInput syndFeedInput = TestUtils.getPropertyValue(source, "syndFeedInput", SyndFeedInput.class);
		assertSame(context.getBean(SyndFeedInput.class), syndFeedInput);
		assertFalse(syndFeedInput.isPreserveWireFeed());
		context.close();
	}


	@Test
	public void validateSuccessfulHttpConfigurationWithCustomMetadataStore() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"FeedInboundChannelAdapterParserTests-http-context.xml", this.getClass());
		SourcePollingChannelAdapter adapter = context.getBean("feedAdapter", SourcePollingChannelAdapter.class);
		FeedEntryMessageSource source = (FeedEntryMessageSource) TestUtils.getPropertyValue(adapter, "source");
		assertNotNull(TestUtils.getPropertyValue(source, "metadataStore"));
		context.close();
	}

	@Test
	public void validateSuccessfulNewsRetrievalWithFileUrlAndMessageHistory() throws Exception {
		//Test file samples.rss has 3 news items
		latch = spy(new CountDownLatch(3));
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"FeedInboundChannelAdapterParserTests-file-usage-context.xml", this.getClass());
		latch.await(10, TimeUnit.SECONDS);
		verify(latch, times(3)).countDown();
		context.close();

		// since we are not deleting the persister file
		// in this iteration no new feeds will be received and the latch will timeout
		latch = spy(new CountDownLatch(3));
		context = new ClassPathXmlApplicationContext(
				"FeedInboundChannelAdapterParserTests-file-usage-context.xml", this.getClass());
		latch.await(500, TimeUnit.MILLISECONDS);
		verify(latch, times(0)).countDown();

		SourcePollingChannelAdapter adapter = context.getBean("feedAdapterUsage", SourcePollingChannelAdapter.class);
		assertTrue(TestUtils.getPropertyValue(adapter, "source.syndFeedInput.preserveWireFeed", Boolean.class));

		context.close();
	}

	@Test
	@Ignore // goes against the real feed
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
		SourcePollingChannelAdapter adapter = context.getBean("autoChannel.adapter", SourcePollingChannelAdapter.class);
		assertSame(autoChannel, TestUtils.getPropertyValue(adapter, "outputChannel"));
		context.close();
	}

	public static class SampleService {

		public void receiveFeedEntry(Message<?> message) {
			MessageHistory history = MessageHistory.read(message);
			assertTrue(history.size() == 3);
			Properties historyItem = history.get(0);
			assertEquals("feedAdapterUsage", historyItem.get("name"));
			assertEquals("feed:inbound-channel-adapter", historyItem.get("type"));

			historyItem = history.get(1);
			assertEquals("feedChannelUsage", historyItem.get("name"));
			assertEquals("channel", historyItem.get("type"));

			historyItem = history.get(2);
			assertEquals("sampleActivator", historyItem.get("name"));
			assertEquals("service-activator", historyItem.get("type"));
			latch.countDown();
		}
	}


	public static class SampleServiceNoHistory {

		public void receiveFeedEntry(SyndEntry entry) {
			latch.countDown();
		}

	}

}
