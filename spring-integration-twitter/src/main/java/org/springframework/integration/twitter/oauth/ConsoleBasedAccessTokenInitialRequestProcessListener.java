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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import org.springframework.beans.factory.config.PropertiesFactoryBean;

import org.springframework.core.io.FileSystemResource;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Default System.(out|in) based implementation of the {@link org.springframework.integration.twitter.oauth.AccessTokenInitialRequestProcessListener} interface
 *
 * @author Josh Long
 */
public class ConsoleBasedAccessTokenInitialRequestProcessListener implements AccessTokenInitialRequestProcessListener {
    public String openUrlAndReturnPin(String urlToOpen)
        throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Open the following URL and grant access to your account:");
        System.out.println(urlToOpen);
        System.out.print("Enter the PIN(if aviailable) or just hit enter.[PIN]:");

        return StringUtils.trim(br.readLine());
    }

    public void persistReturnedAccessToken(AccessToken accessToken)
        throws Exception {
        Map<String, String> output = new HashMap<String, String>();
        output.put(OAuthConfigurationFactoryBean.WELL_KNOWN_CONSUMER_ACCESS_TOKEN, accessToken.getToken());
        output.put(OAuthConfigurationFactoryBean.WELL_KNOWN_CONSUMER_ACCESS_TOKEN_SECRET, accessToken.getTokenSecret());

        File accessTokenCreds = new File(SystemUtils.getJavaIoTmpDir(), "twitter-accesstoken.properties");
        FileOutputStream fileOutputStream = new FileOutputStream(accessTokenCreds);
        Properties props = new Properties();
        props.putAll(output);
        props.store(fileOutputStream, "oauth-access-token");
        IOUtils.closeQuietly(fileOutputStream);

        System.out.println("The oauth accesstoken credentials have been written to " + accessTokenCreds.getAbsolutePath());
    }

    public void failure(Throwable t) {
        System.err.println("Exception occurred when trying to retrieve credentials: " + ExceptionUtils.getFullStackTrace(t));
    }

    public static void main(String[] args) throws Exception {
        File twitterProps = new File(SystemUtils.getUserHome(), "Desktop/twitter.properties");

        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new FileSystemResource(twitterProps));
	    propertiesFactoryBean.afterPropertiesSet() ;	    
        Properties props = propertiesFactoryBean.getObject();

        String key = StringUtils.trim(props.getProperty("twitter.oauth.consumerKey"));
        String secret = StringUtils.trim(  props.getProperty("twitter.oauth.consumerSecret") ) ;

        ConsoleBasedAccessTokenInitialRequestProcessListener consoleBasedAccessTokenInitialRequestProcessListener =
				new ConsoleBasedAccessTokenInitialRequestProcessListener();

        Twitter twitter = new TwitterFactory().getOAuthAuthorizedInstance( key, secret);

        RequestToken requestToken = twitter.getOAuthRequestToken();
        AccessToken accessToken = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (null == accessToken) {
            String pin = consoleBasedAccessTokenInitialRequestProcessListener.openUrlAndReturnPin(requestToken.getAuthorizationURL());

            try {
                if (pin.length() > 0) {
                    accessToken = twitter.getOAuthAccessToken(requestToken, pin);
                } else {
                    accessToken = twitter.getOAuthAccessToken();
                }
            } catch (TwitterException te) {
                if (401 == te.getStatusCode()) {
                    System.out.println("Unable to get the access token.");
                } else {
                    te.printStackTrace();
                }
            }
        }

        consoleBasedAccessTokenInitialRequestProcessListener.persistReturnedAccessToken(accessToken);

    }
}
