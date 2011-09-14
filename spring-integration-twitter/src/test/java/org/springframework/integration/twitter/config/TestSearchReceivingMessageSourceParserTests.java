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

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.twitter.inbound.SearchReceivingMessageSource;
import org.springframework.social.twitter.api.Twitter;

import static org.junit.Assert.assertNotNull;


/**
 * @author Oleg Zhurakousky
 */
public class TestSearchReceivingMessageSourceParserTests {

	@Test
	@Ignore // becouse userOpoeration.getProfile() throws exception where it doesn't have to since its a seqrch
	public void testSearchReceivingDefaultTemplate(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("TestSearchReceivingMessageSourceParser-context.xml", this.getClass());
		SourcePollingChannelAdapter spca = ac.getBean("searchAdapter", SourcePollingChannelAdapter.class);
		SearchReceivingMessageSource ms = (SearchReceivingMessageSource) TestUtils.getPropertyValue(spca, "source");
		Twitter template = (Twitter) TestUtils.getPropertyValue(ms, "twitter");
		assertNotNull(template);
	}
}
