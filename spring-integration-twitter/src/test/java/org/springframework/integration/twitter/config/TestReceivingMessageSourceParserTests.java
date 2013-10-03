/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.twitter.inbound.DirectMessageReceivingMessageSource;
import org.springframework.integration.twitter.inbound.MentionsReceivingMessageSource;
import org.springframework.integration.twitter.inbound.TimelineReceivingMessageSource;

import static org.junit.Assert.assertNotNull;


/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 */
public class TestReceivingMessageSourceParserTests {

	@Test
	public void testReceivingAdapterConfigurationAutoStartup(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("TestReceivingMessageSourceParser-context.xml", this.getClass());
		SourcePollingChannelAdapter spca = ac.getBean("mentionAdapter", SourcePollingChannelAdapter.class);
		MentionsReceivingMessageSource ms = TestUtils.getPropertyValue(spca, "source", MentionsReceivingMessageSource.class);
		assertNotNull(ms);

		spca = ac.getBean("dmAdapter", SourcePollingChannelAdapter.class);
		DirectMessageReceivingMessageSource dms = TestUtils.getPropertyValue(spca, "source", DirectMessageReceivingMessageSource.class);
		assertNotNull(dms);

		spca = ac.getBean("updateAdapter", SourcePollingChannelAdapter.class);

		spca = ac.getBean("updateAdapter", SourcePollingChannelAdapter.class);
		TimelineReceivingMessageSource tms = TestUtils.getPropertyValue(spca, "source", TimelineReceivingMessageSource.class);
		assertNotNull(tms);
	}

	@Test
	public void testThatMessageSourcesAreRegisteredAsBeans(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("TestReceivingMessageSourceParser-context.xml", this.getClass());

		MentionsReceivingMessageSource ms = ac.getBean("mentionAdapter.source", MentionsReceivingMessageSource.class);
		assertNotNull(ms);

		DirectMessageReceivingMessageSource dms = ac.getBean("dmAdapter.source", DirectMessageReceivingMessageSource.class);
		assertNotNull(dms);

		TimelineReceivingMessageSource tms = ac.getBean("updateAdapter.source", TimelineReceivingMessageSource.class);
		assertNotNull(tms);
	}

	@Test
	public void testThatMessageSourcesAreRegisteredAsBeansWithoutIds(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("TestReceivingMessageSourceParserWithoutIds-context.xml", this.getClass());

		final String mentionAdapterSourceId = "org.springframework.integration.twitter.inbound.MentionsReceivingMessageSource#0.source";
		MentionsReceivingMessageSource ms = ac.getBean(mentionAdapterSourceId, MentionsReceivingMessageSource.class);
		assertNotNull(ms);

		final String dmAdapterSourceId = "org.springframework.integration.twitter.inbound.DirectMessageReceivingMessageSource#0.source";
		DirectMessageReceivingMessageSource dms = ac.getBean(dmAdapterSourceId, DirectMessageReceivingMessageSource.class);
		assertNotNull(dms);

		final String updateAdapterSourceId = "org.springframework.integration.twitter.inbound.TimelineReceivingMessageSource#0.source";
		TimelineReceivingMessageSource tms = ac.getBean(updateAdapterSourceId, TimelineReceivingMessageSource.class);
		assertNotNull(tms);
	}

}
