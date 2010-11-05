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
package org.springframework.integration.twitter.oauth;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;

/**
 * Will create an OAuth-Authorized instance of Twitter object.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class OAuthTwitterFactoryBean implements FactoryBean<Twitter>, InitializingBean {
	private final String consumerKey;
	private final String consumerSecret;
	private final AccessToken accessToken;
	
	private volatile Twitter twitter;
	
	public OAuthTwitterFactoryBean(String consumerKey, String consumerSecret, AccessToken accessToken){
		Assert.hasText(consumerKey, "'consumerKey' must be provided");
		Assert.hasText(consumerSecret, "'consumerSecret' must be provided");
		Assert.notNull(accessToken, "'accessToken' must be provided");
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.accessToken = accessToken;
	}
	@Override
	public Twitter getObject() throws Exception {
		Assert.notNull(this.twitter, "OAuthTwitterFactoryBean must be initialized. Invoke afterPropertiesSet() method");
		return twitter;
	}

	@Override
	public Class<?> getObjectType() {
		return Twitter.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.twitter = new TwitterFactory().getOAuthAuthorizedInstance(consumerKey, consumerSecret, accessToken);
	}

}
