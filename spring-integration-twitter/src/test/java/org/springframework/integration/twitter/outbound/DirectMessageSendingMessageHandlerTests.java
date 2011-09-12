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

package org.springframework.integration.twitter.outbound;

import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.twitter.core.TwitterHeaders;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 */
public class DirectMessageSendingMessageHandlerTests {

	private Twitter twitter = mock(Twitter.class);
	
	//private TwitterOperations twitter = mock(TwitterOperations.class);

//	@Test
//	public void validateSendDirectMessage() throws Exception{
//		Message<?> message1 = MessageBuilder.withPayload("Migrating SI Twitter to Spring Social")
//				.setHeader(TwitterHeaders.DM_TARGET_USER_ID, "z_oleg").build();
//		DirectMessageSendingMessageHandler handler = new DirectMessageSendingMessageHandler(twitter);
//		handler.afterPropertiesSet();
//		handler.handleMessage(message1);
//		verify(twitter, times(1)).sendDirectMessage("foo", "hello");
//		Message<?> message2 = MessageBuilder.withPayload("hello")
//				.setHeader(TwitterHeaders.DM_TARGET_USER_ID, 123).build();;
//		handler.handleMessage(message2);
//		verify(twitter, times(1)).sendDirectMessage(123, "hello");
//	}
	
	@Test @Ignore
	public void validateSendDirectMessage() throws Exception{
		PropertiesFactoryBean pf = new PropertiesFactoryBean();
		pf.setLocation(new ClassPathResource("sample.properties"));
		pf.afterPropertiesSet();
		Properties prop =  pf.getObject();
		System.out.println(prop);
		TwitterTemplate template = new TwitterTemplate(prop.getProperty("twitter.oauth.consumerKey"), 
										               prop.getProperty("twitter.oauth.consumerSecret"), 
										               prop.getProperty("twitter.oauth.accessToken"), 
										               prop.getProperty("twitter.oauth.accessTokenSecret"));
		Message<?> message1 = MessageBuilder.withPayload("Migrating SI Twitter to Spring Social")
				.setHeader(TwitterHeaders.DM_TARGET_USER_ID, "z_oleg").build();
		DirectMessageSendingMessageHandler handler = new DirectMessageSendingMessageHandler(template);
		handler.afterPropertiesSet();
		handler.handleMessage(message1);
	}
	
}
