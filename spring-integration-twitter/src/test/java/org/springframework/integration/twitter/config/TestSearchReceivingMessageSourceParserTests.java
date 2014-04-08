/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.twitter.inbound.SearchReceivingMessageSource;
import org.springframework.social.twitter.api.Twitter;


/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class TestSearchReceivingMessageSourceParserTests {

	@Test
	public void testSearchReceivingDefaultTemplate(){
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext(
				"TestSearchReceivingMessageSourceParser-context.xml", this.getClass());
		SourcePollingChannelAdapter spca = ac.getBean("searchAdapterWithTemplate", SourcePollingChannelAdapter.class);
		SearchReceivingMessageSource ms = (SearchReceivingMessageSource) TestUtils.getPropertyValue(spca, "source");
		assertEquals(Integer.valueOf(23), TestUtils.getPropertyValue(ms, "pageSize", Integer.class));
		Twitter template = (Twitter) TestUtils.getPropertyValue(ms, "twitter");
		assertNotNull(template);
		ac.close();
	}
}
