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
package org.springframework.integration.feed.config;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.feed.FeedEntryReaderMessageSource;
import org.springframework.integration.feed.FeedReaderMessageSource;
import org.springframework.integration.feed.FileUrlFeedFetcher;
import org.springframework.integration.test.util.TestUtils;

import com.sun.syndication.fetcher.impl.AbstractFeedFetcher;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;


/**
 * @author Oleg Zhurakousky
 *
 */
public class FeedMessageSourceBeanDefinitionParserTests {

	@Test
	public void validateSuccessfullConfiguration(){
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("FeedMessageSourceBeanDefinitionParserTests-file-context.xml", this.getClass());
		SourcePollingChannelAdapter adapter = context.getBean("feedAdapter", SourcePollingChannelAdapter.class);
		FeedEntryReaderMessageSource source = (FeedEntryReaderMessageSource) TestUtils.getPropertyValue(adapter, "source");
		FeedReaderMessageSource feedReaderMessageSource = (FeedReaderMessageSource) TestUtils.getPropertyValue(source, "feedReaderMessageSource");
		AbstractFeedFetcher fetcher = (AbstractFeedFetcher) TestUtils.getPropertyValue(feedReaderMessageSource, "fetcher");
		assertTrue(fetcher instanceof FileUrlFeedFetcher);
		
		context = 
			new ClassPathXmlApplicationContext("FeedMessageSourceBeanDefinitionParserTests-http-context.xml", this.getClass());
		adapter = context.getBean("feedAdapter", SourcePollingChannelAdapter.class);
		source = (FeedEntryReaderMessageSource) TestUtils.getPropertyValue(adapter, "source");
		feedReaderMessageSource = (FeedReaderMessageSource) TestUtils.getPropertyValue(source, "feedReaderMessageSource");
		fetcher = (AbstractFeedFetcher) TestUtils.getPropertyValue(feedReaderMessageSource, "fetcher");
		assertTrue(fetcher instanceof HttpURLFeedFetcher);
	}
	@Test
	public void validateSuccessfullNewsRetrievalFile() throws Exception{
		//Test file samples.rss has 3 news items
		final CountDownLatch latch = new CountDownLatch(3);
		MessageHandler handler = spy(new MessageHandler() {		
			public void handleMessage(Message<?> message) throws MessagingException {
				latch.countDown();
			}
		});
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("FeedMessageSourceBeanDefinitionParserTests-file-context.xml", this.getClass());
		DirectChannel feedChannel = context.getBean("feedChannel", DirectChannel.class);	
		feedChannel.subscribe(handler);
		latch.await(5, TimeUnit.SECONDS);
		verify(handler, times(3)).handleMessage(Mockito.any(Message.class));
	}
	@Test
	public void validateSuccessfullNewsRetrievalHttp() throws Exception{
		//Test file samples.rss has 3 news items
		final CountDownLatch latch = new CountDownLatch(3);
		MessageHandler handler = spy(new MessageHandler() {		
			public void handleMessage(Message<?> message) throws MessagingException {
				latch.countDown();
			}
		});
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("FeedMessageSourceBeanDefinitionParserTests-http-context.xml", this.getClass());
		DirectChannel feedChannel = context.getBean("feedChannel", DirectChannel.class);	
		feedChannel.subscribe(handler);
		latch.await(5, TimeUnit.SECONDS);
		verify(handler, atLeast(3)).handleMessage(Mockito.any(Message.class));
	}
}
