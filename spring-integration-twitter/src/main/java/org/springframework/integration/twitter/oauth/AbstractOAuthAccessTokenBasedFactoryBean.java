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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;

import java.util.Properties;


/**
 * base-class for {@link org.springframework.integration.twitter.oauth.OAuthAccessTokenBasedTwitterFactoryBean}.
 * <p/>
 * Provides hooks so that subclasses able to reference a concrete implementation of the generic type parameter can call methods for us. Most hooks are to handle the case
 * of the first time subscription, where no accessToken is present.
 *
 * @author Josh Long
 * @param <T>
 * @see org.springframework.integration.twitter.oauth.OAuthAccessTokenBasedTwitterFactoryBean
 * @since 2.0
 */
abstract public class AbstractOAuthAccessTokenBasedFactoryBean<T> implements InitializingBean, FactoryBean<T> {
	protected OAuthConfiguration configuration;
	protected final Object monitor = new Object();
	protected volatile T twitter;
//	protected volatile AccessTokenInitialRequestProcessListener accessTokenInitialRequestProcessListener;
	protected volatile boolean initialized = false;

	/**
	 * Nasty little bit of circular indirection here: the {@link org.springframework.integration.twitter.oauth.OAuthConfiguration} hosts the String values for authentication,
	 * which we need to build up this instance, but the {@link org.springframework.integration.twitter.oauth.OAuthConfiguration} in turn needs references to the instances provided by
	 * this {@link org.springframework.beans.factory.FactoryBean}. So, they collaborate and guard each others state. Ultimately, clients should use {@link org.springframework.integration.twitter.oauth.OAuthConfiguration}
	 * to correctly any implementations of this factory bean as well as the {@link org.springframework.integration.twitter.oauth.OAuthConfiguration} reference itself.
	 *
	 * @param configuration the configuration object
	 */
	protected AbstractOAuthAccessTokenBasedFactoryBean(OAuthConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * This probably doesn't belong here. It's more for support for running the {@link OAuthAccessTokenBasedTwitterFactoryBean#main(String[])}  or
	 * {@link OAuthAccessTokenBasedTwitterFactoryBean#main(String[])} methods that run the user through a command line tool to approve a user for the first
	 * time if the user hasn't obtained her {@code accessToken } yet
	 *
	 * @param resource the resource where properties file lives
	 * @return returns a fully configured {@link java.util.Properties} instance
	 * @throws Exception thrown if anythign goes wrong
	 */
	@SuppressWarnings("unused")
	protected static Properties fromResource(Resource resource)
			throws Exception {
		PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
		propertiesFactoryBean.setLocation(resource);
		propertiesFactoryBean.setSingleton(true);
		propertiesFactoryBean.afterPropertiesSet();

		return propertiesFactoryBean.getObject();
	}

	/**
	 * provides lifecycle for initiation of the reference. By the time this method is left we should have a fully configured twitter connection that can connect and make calls
	 *
	 * @throws Exception
	 */
	public void afterPropertiesSet() throws Exception {
		synchronized (this.monitor) {
			/*if (this.accessTokenInitialRequestProcessListener == null) {
				accessTokenInitialRequestProcessListener = new ConsoleBasedAccessTokenInitialRequestProcessListener();
			}*/

			Assert.notNull(this.configuration.getConsumerKey(), "'consumerKey' mustn't be null");
			Assert.notNull(this.configuration.getConsumerSecret(), "'consumerSecret' mustn't be null");

			AccessToken accessTokenObj=null;
		    establishTwitterObject(accessTokenObj);
			if (StringUtils.hasText(this.configuration.getAccessToken()) && StringUtils.hasText(this.configuration.getAccessTokenSecret())) {
				accessTokenObj = new AccessToken(this.configuration.getAccessToken(), this.configuration.getAccessTokenSecret());
			} /*else {
			//	accessTokenObj = initialAuthorizationWizard();
			}*/

			establishTwitterObject(accessTokenObj);

			Assert.notNull(accessTokenObj, "'accessTokenObj' can't be null");

			this.initialized = true;
		}
	}

	/*@SuppressWarnings("unused")
	public void setAccessTokenInitialRequestProcessListener(AccessTokenInitialRequestProcessListener accessTokenInitialRequestProcessListener) {
		this.accessTokenInitialRequestProcessListener = accessTokenInitialRequestProcessListener;
	}
*/
	public abstract void establishTwitterObject(AccessToken accessToken)
			throws Exception;

	/**
	 * because we are not able to dereference the {@link twitter4j.Twitter} or {@link twitter4j.AsyncTwitter} instances, we need to ask subclasses to call
	 * us how to call {@link twitter4j.AsyncTwitter#getOAuthRequestToken()} or {@link twitter4j.Twitter#getOAuthRequestToken()} for us.This method
	 * will never be evaluated as long as the {@link org.springframework.integration.twitter.oauth.OAuthConfiguration#accessToken}
	 * and {@link org.springframework.integration.twitter.oauth.OAuthConfiguration#accessTokenSecret} beans are not null.
	 *
	 * @return the {@link twitter4j.http.RequestToken} as vended by the service. Ths will contain a verification URl required to obtain an access key and secret.
	 * @throws Exception thrown if anything should go wrong
	 */
	public abstract RequestToken getOAuthRequestToken()
			throws Exception;

	/**
	 * Only used if the impementation is trying to get an {@link twitter4j.http.AccessToken} for the first time. This method
	 * will never be evaluated as long as the {@link org.springframework.integration.twitter.oauth.OAuthConfiguration#accessToken}
	 * and {@link org.springframework.integration.twitter.oauth.OAuthConfiguration#accessTokenSecret} beans are not null.
	 *
	 * @param token the initiating {@link twitter4j.http.RequestToken}
	 * @param pin   the string returned from the verification URL
	 * @return returns the {@link twitter4j.http.AccessToken} fetched from the twitter service.
	 * @throws Exception thrown if anything should go wrong
	 */
	public abstract AccessToken getOAuthAccessToken(RequestToken token, String pin)
			throws Exception;

	/**
	 * Only used if the impementation is trying to get an {@link twitter4j.http.AccessToken} for the first time. This method
	 * will never be evaluated as long as the {@link org.springframework.integration.twitter.oauth.OAuthConfiguration#accessToken}
	 * and {@link org.springframework.integration.twitter.oauth.OAuthConfiguration#accessTokenSecret} beans are not null.
	 *
	 * @return returns the {@link twitter4j.http.AccessToken} fetched from the twitter service.
	 * @throws Exception thrown if anything should go wrong
	 */
	public abstract AccessToken getOAuthAccessToken() throws Exception;

	/**
	 * @return returns the freshly created {@link twitter4j.http.AccessToken} object from the service
	 * @throws Exception for just about any deviation from the expected
	 */
/*
	private AccessToken initialAuthorizationWizard() throws Exception {
		Assert.notNull(this.accessTokenInitialRequestProcessListener, "'accessTokenInitialRequestProcessListener' can't be null");

		try {
			RequestToken requestToken = getOAuthRequestToken();
			String pin = this.accessTokenInitialRequestProcessListener.openUrlAndReturnPin(requestToken.getAuthorizationURL());
			AccessToken at = StringUtils.hasText(pin) ? getOAuthAccessToken(requestToken, pin) : getOAuthAccessToken();
			this.accessTokenInitialRequestProcessListener.persistReturnedAccessToken(at);

			return at;
		} catch (Throwable th) {
			this.accessTokenInitialRequestProcessListener.failure(th);
		}

		return null;
	}
*/

	/**
	 * Responsibility of subclasses to call this because we cant dereference the generic type appropriately. The responsibility is
	 * to call {@link twitter4j.Twitter#verifyCredentials()} or {@link twitter4j.AsyncTwitter#verifyCredentials()}  as appropriate
	 *
	 * @throws Exception if there's an inability to authenticate
	 */
	public abstract void verifyCredentials() throws Exception;

	/**
	 * Rubber meets the road: builds up a reference to the twitter4j.(Async)Twitter instance
	 *
	 * @return the instance
	 * @throws Exception thrown in case some condition isn't met correctly in construction
	 */
	public T getObject() throws Exception {
		if (!initialized) {
			afterPropertiesSet();
		}

		return this.twitter;
	}

	/**
	 * this method is delegated to implementations because we can't correctly dereference the generic type's class
	 *
	 * @return a class
	 */
	abstract public Class<?> getObjectType();

	/**
	 * Standard {@link org.springframework.beans.factory.FactoryBean} method. Implementations may override if there's a specific method
	 *
	 * @return whether or not this is a singleton
	 */
	public boolean isSingleton() {
		return true;
	}
}
