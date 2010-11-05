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
package org.springframework.integration.twitter.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.twitter.oauth.OAuthTwitterFactoryBean;

import twitter4j.Twitter;
import twitter4j.http.AccessToken;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 *
 */
public class TwitterConnectionParserTests {

	@Test
	public void testOAuthTwitterFactoryBean(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("TwitterConnectionParserTests-context.xml", this.getClass());
		OAuthTwitterFactoryBean twitterFb = ac.getBean("&twitter", OAuthTwitterFactoryBean.class);
		assertEquals("consumerKey", TestUtils.getPropertyValue(twitterFb, "consumerKey"));
		assertEquals("consumerSecret", TestUtils.getPropertyValue(twitterFb, "consumerSecret"));
		AccessToken accessToken = (AccessToken) TestUtils.getPropertyValue(twitterFb, "accessToken");
		assertEquals("accessToken", accessToken.getToken());
		assertEquals("accessTokenSecret", accessToken.getTokenSecret());
		Twitter twitter = ac.getBean("twitter", Twitter.class);
		assertTrue(twitter.isOAuthEnabled());
	}
}
