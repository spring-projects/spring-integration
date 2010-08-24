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

import twitter4j.Twitter;


/**
 * Simple bean we can use to store the configuration and store shared references to both an {@link twitter4j.AsyncTwitter}
 * and an {@link twitter4j.Twitter} instance.
 * <p/>
 * client should store this bean and simply lookup the Twitter configuration from there
 */
public class OAuthConfiguration {
    //
    // private AsyncTwitter asyncTwitter;
    private Twitter twitter;
    private volatile String consumerKey;
    private volatile String consumerSecret;
    private volatile String accessToken;
    private volatile String accessTokenSecret;

    public OAuthConfiguration(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.accessToken = accessToken;
        this.accessTokenSecret = accessTokenSecret;
    }

    /** package friendly */
    /*  void setAsyncTwitter(AsyncTwitter asyncTwitter) {
          this.asyncTwitter = asyncTwitter;
      }*/

    /** package friendly */
    void setTwitter(Twitter twitter) {
        this.twitter = twitter;
    }

    /**
     * @return
     */
    public Twitter getTwitter() {
        return twitter;
    }

    /*
        public AsyncTwitter getAsyncTwitter() {
            return asyncTwitter;
        }*/
    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
    }
}
