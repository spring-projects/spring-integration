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

package org.springframework.integration.twitter.inbound;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.twitter.core.Tweet;
import org.springframework.integration.twitter.core.Twitter4jTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


/**
 * @author Oleg Zhurakousky
 */
public class SearchReceivingMessageSourceTests {

	@Test
	@Ignore
	public void testSearchReceiving() throws Exception{
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ThreadPoolTaskScheduler scheduler =  new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		bf.registerSingleton("taskScheduler", scheduler);
		SearchReceivingMessageSource ms = new SearchReceivingMessageSource(new Twitter4jTemplate());
		DirectChannel channel = new DirectChannel(); 
		channel.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				System.out.println("Message: " + ((Tweet)message.getPayload()).getCreatedAt() + " - " + ((Tweet)message.getPayload()).getText());
			}
		});
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		adapter.setSource(ms);
		adapter.setBeanFactory(bf);
		adapter.setOutputChannel(channel);
		adapter.afterPropertiesSet();
		adapter.start();
		ms.setBeanFactory(bf);
		ms.setQuery("#springintegration");
		//ms.setTaskScheduler(scheduler);
		ms.afterPropertiesSet();
		//ms.start();
		System.in.read();
	}

}
