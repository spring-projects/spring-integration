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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.twitter.oauth.OAuthConfiguration;

import twitter4j.Twitter;

/**
 * @author Oleg Zhurakousky
 *
 */
public class TestSendingMessageHandlerParserTests {

	@Test
	public void testSendingMessageHandlerSuccessfullBootstrap(){
		new ClassPathXmlApplicationContext("TestSendingMessageHandlerParser-context.xml", this.getClass());
		// the fact that no exception was thrown satisfies this test	
	}
	
	public static class MockOathConfigurationFactoryBean implements FactoryBean<OAuthConfiguration>{

		@Override
		public OAuthConfiguration getObject() throws Exception {
			OAuthConfiguration config = mock(OAuthConfiguration.class);
			Twitter twitter = mock(Twitter.class);
			when(config.getTwitter()).thenReturn(twitter);
			return config;
		}

		@Override
		public Class<?> getObjectType() {
			return OAuthConfiguration.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}	
	}
}
