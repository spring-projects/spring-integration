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
import org.springframework.messaging.Message;
import org.springframework.social.twitter.api.DirectMessage;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

/**
 * @author Oleg Zhurakousky
 */
public class DirectMessageReceivingMessageSourceTests {


	@SuppressWarnings("unchecked")
	@Test @Ignore
	public void demoReceiveDm() throws Exception{
		PropertiesFactoryBean pf = new PropertiesFactoryBean();
		pf.setLocation(new ClassPathResource("sample.properties"));
		pf.afterPropertiesSet();
		Properties prop =  pf.getObject();
		System.out.println(prop);
		TwitterTemplate template = new TwitterTemplate(prop.getProperty("z_oleg.oauth.consumerKey"),
										               prop.getProperty("z_oleg.oauth.consumerSecret"),
										               prop.getProperty("z_oleg.oauth.accessToken"),
										               prop.getProperty("z_oleg.oauth.accessTokenSecret"));
		DirectMessageReceivingMessageSource tSource = new DirectMessageReceivingMessageSource(template, "foo");
		tSource.afterPropertiesSet();
		for (int i = 0; i < 50; i++) {
			Message<DirectMessage> message = (Message<DirectMessage>) tSource.receive();
			if (message != null){
				DirectMessage tweet = message.getPayload();
				System.out.println(tweet.getSender().getScreenName() + " - " + tweet.getText() + " - " + tweet.getCreatedAt());
			}
		}
	}
}
