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

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.core.io.FileSystemResource;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;

import java.io.File;
import java.util.Properties;


public class OAuthAccessTokenBasedTwitterFactoryBean extends AbstractOAuthAccessTokenBasedFactoryBean<Twitter> {
	protected OAuthAccessTokenBasedTwitterFactoryBean(OAuthConfiguration configuration) {
		super(configuration);
	}

	@Override
	public void establishTwitterObject(AccessToken accessToken)
			throws Exception {
		this.twitter = new TwitterFactory().getOAuthAuthorizedInstance(this.configuration.getConsumerKey(), this.configuration.getConsumerSecret(), accessToken);
	}

	@Override
	public RequestToken getOAuthRequestToken() throws Exception {
		return twitter.getOAuthRequestToken();
	}

	@Override
	public void verifyCredentials() throws Exception {
		this.twitter.verifyCredentials();
	}

	@Override
	public AccessToken getOAuthAccessToken(RequestToken token, String pin)
			throws Exception {
		return this.twitter.getOAuthAccessToken(token, pin);
	}

	@Override
	public AccessToken getOAuthAccessToken() throws Exception {
		return this.twitter.getOAuthAccessToken();
	}

	@Override
	public Class<?> getObjectType() {
		return Twitter.class;
	}

	public static void main(String[] args) throws Throwable {
		File propsFile = new File(new File(SystemUtils.getUserHome(), "Desktop"), "twitter.properties");
		FileSystemResource fileSystemResource = new FileSystemResource(propsFile);
		Properties properties = fromResource(fileSystemResource);

		OAuthConfigurationFactoryBean oAuthConfigurationFactoryBean = new OAuthConfigurationFactoryBean();
		oAuthConfigurationFactoryBean.bootstrapFromProperties(properties);

		OAuthConfiguration configuration = oAuthConfigurationFactoryBean.getObject();

		Twitter twitter = configuration.getTwitter();

		System.out.println("Used the " + OAuthAccessTokenBasedTwitterFactoryBean.class.getName() + " and arrived at " + ToStringBuilder.reflectionToString(twitter));

		ResponseList<Status> friendsTimeline = twitter.getFriendsTimeline();
		System.out.println("Friends' timeline " + friendsTimeline);
		Thread.sleep(10000);
	}
}
