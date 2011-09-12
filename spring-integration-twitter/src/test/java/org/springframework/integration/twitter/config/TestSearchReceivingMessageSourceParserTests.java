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


/**
 * @author Oleg Zhurakousky
 */
public class TestSearchReceivingMessageSourceParserTests {

//	@Test
//	public void testSearchReceivingDefaultTemplate(){
//		ApplicationContext ac = new ClassPathXmlApplicationContext("TestSearchReceivingMessageSourceParser-context.xml", this.getClass());
//		SourcePollingChannelAdapter spca = ac.getBean("searchAdapter", SourcePollingChannelAdapter.class);
//		SearchReceivingMessageSource ms = (SearchReceivingMessageSource) TestUtils.getPropertyValue(spca, "source");
//		//assertFalse(ms.isAutoStartup());
//		Twitter4jTemplate template = (Twitter4jTemplate) TestUtils.getPropertyValue(ms, "twitterOperations");
//		assertFalse(template.getUnderlyingTwitter().isOAuthEnabled()); // verify anonymous Twitter
//	}
//
//	@Test
//	public void testSearchReceivingCustomTemplate(){
//		ApplicationContext ac = new ClassPathXmlApplicationContext("TestSearchReceivingMessageSourceParser-context.xml", this.getClass());
//		SourcePollingChannelAdapter spca = ac.getBean("searchAdapterWithTemplate", SourcePollingChannelAdapter.class);
//		SearchReceivingMessageSource ms = (SearchReceivingMessageSource) TestUtils.getPropertyValue(spca, "source");
//		//assertFalse(ms.isAutoStartup());
//		TwitterOperations template = (TwitterOperations) TestUtils.getPropertyValue(ms, "twitterOperations");
//		assertEquals(ac.getBean("twitter"), template);
//	}

}
