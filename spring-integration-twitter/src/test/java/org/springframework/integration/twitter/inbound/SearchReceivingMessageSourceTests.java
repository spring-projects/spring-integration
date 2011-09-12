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

import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.Message;
import org.springframework.social.twitter.api.DirectMessage;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.impl.TwitterTemplate;


/**
 * @author Oleg Zhurakousky
 */
public class SearchReceivingMessageSourceTests {

//	/**
//	 * THis test is a sample test and wil require connecting to a real Twitter
//	 * however no OAuth is required sincxe uts a search, so simply uncomment and run
//	 * @throws Exception
//	 */
//	@Test
//	@Ignore
//	public void testSearchReceiving() throws Exception{
//		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
//		ThreadPoolTaskScheduler scheduler =  new ThreadPoolTaskScheduler();
//		scheduler.afterPropertiesSet();
//		bf.registerSingleton("taskScheduler", scheduler);
//		SearchReceivingMessageSource ms = new SearchReceivingMessageSource(new Twitter4jTemplate());
//		DirectChannel channel = new DirectChannel(); 
//		channel.subscribe(new MessageHandler() {
//			public void handleMessage(Message<?> message) throws MessagingException {
//				System.out.println("Message: " + ((Tweet)message.getPayload()).getCreatedAt() + " - " + ((Tweet)message.getPayload()).getText());
//			}
//		});
//		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
//		adapter.setSource(ms);
//		adapter.setBeanFactory(bf);
//		adapter.setOutputChannel(channel);
//		adapter.afterPropertiesSet();
//		adapter.start();
//		ms.setBeanFactory(bf);
//		ms.setQuery("#springintegration");
//		//ms.setTaskScheduler(scheduler);
//		ms.afterPropertiesSet();
//		//ms.start();
//		System.in.read();
//	}
	
	@SuppressWarnings("unchecked")
	@Test @Ignore
	public void demoReceiveSearchResults() throws Exception{
		PropertiesFactoryBean pf = new PropertiesFactoryBean();
		pf.setLocation(new ClassPathResource("sample.properties"));
		pf.afterPropertiesSet();
		Properties prop =  pf.getObject();
		System.out.println(prop);
		TwitterTemplate template = new TwitterTemplate(prop.getProperty("z_oleg.oauth.consumerKey"), 
										               prop.getProperty("z_oleg.oauth.consumerSecret"), 
										               prop.getProperty("z_oleg.oauth.accessToken"), 
										               prop.getProperty("z_oleg.oauth.accessTokenSecret"));
		SearchReceivingMessageSource tSource = new SearchReceivingMessageSource(template);
		tSource.setQuery("#springsocial");
		tSource.afterPropertiesSet();
		for (int i = 0; i < 50; i++) {
			Message<Tweet> message = (Message<Tweet>) tSource.receive();
			if (message != null){
				Tweet tweet = message.getPayload();
				System.out.println(tweet.getFromUser() + " - " + tweet.getText() + " - " + tweet.getCreatedAt());
			}
		}
	}

}
