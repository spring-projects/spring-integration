/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.twitter.oauth;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import twitter4j.Twitter;

import java.util.Properties;


/**
 * Center piece for configuration for all the twitter adapters
 *
 * @author Josh Long
 * @since 2.0
 */
public class OAuthConfigurationFactoryBean implements FactoryBean<OAuthConfiguration> {
	public static final String WELL_KNOWN_CONSUMER_KEY = "twitter.oauth.consumerKey";
	public static final String WELL_KNOWN_CONSUMER_KEY_SECRET = "twitter.oauth.consumerSecret";
	public static final String WELL_KNOWN_CONSUMER_ACCESS_TOKEN = "twitter.oauth.accessToken";
	public static final String WELL_KNOWN_CONSUMER_ACCESS_TOKEN_SECRET = "twitter.oauth.accessTokenSecret";
	private volatile String consumerKey;
	private volatile String consumerSecret;
	private volatile String accessToken;
	private volatile String accessTokenSecret;
	private Twitter twitter;
	private volatile OAuthConfiguration oAuthConfiguration;
	private final Object guard = new Object();

	private Twitter twitter(OAuthConfiguration oAuthConfiguration)
			throws Exception {
		OAuthAccessTokenBasedTwitterFactoryBean twitterFactoryBean = new OAuthAccessTokenBasedTwitterFactoryBean(oAuthConfiguration);
		twitterFactoryBean.afterPropertiesSet();
		twitterFactoryBean.verifyCredentials();

		return twitterFactoryBean.getObject();
	}

	protected void bootstrapFromProperties(Properties props)
			throws Throwable {
		Assert.notNull(props, "'properties' must not be null");
		this.setAccessToken(props.getProperty(WELL_KNOWN_CONSUMER_ACCESS_TOKEN));
		this.setAccessTokenSecret(props.getProperty(WELL_KNOWN_CONSUMER_ACCESS_TOKEN_SECRET));
		this.setConsumerKey(props.getProperty(WELL_KNOWN_CONSUMER_KEY));
		this.setConsumerSecret(props.getProperty(WELL_KNOWN_CONSUMER_KEY_SECRET));
	}

	public OAuthConfiguration getObject() throws Exception {
		return build();
	}

	public Class<?> getObjectType() {
		return OAuthConfiguration.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void setConsumerKey(String consumerKey) {
		this.consumerKey = consumerKey;
	}

	public void setConsumerSecret(String consumerSecret) {
		this.consumerSecret = consumerSecret;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public void setAccessTokenSecret(String accessTokenSecret) {
		this.accessTokenSecret = accessTokenSecret;
	}

	private OAuthConfiguration build() throws Exception {
		synchronized (this.guard) {
			if (oAuthConfiguration == null) {
				oAuthConfiguration = new OAuthConfiguration(this.consumerKey, this.consumerSecret, this.accessToken, this.accessTokenSecret);
				twitter = this.twitter(oAuthConfiguration);
				oAuthConfiguration.setTwitter(twitter);
			}
		}

		return this.oAuthConfiguration;
	}
}
