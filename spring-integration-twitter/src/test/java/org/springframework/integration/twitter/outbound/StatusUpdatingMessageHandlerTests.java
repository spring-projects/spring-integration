/*
 * Copyright 2002-2014 the original author or authors
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

package org.springframework.integration.twitter.outbound;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.social.twitter.api.TimelineOperations;
import org.springframework.social.twitter.api.TweetData;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class StatusUpdatingMessageHandlerTests {

	@Autowired
	MessageChannel in1;

	@Autowired
	MessageChannel in2;

	@Autowired
	Twitter twitter;

	@Test @Ignore
	public void demoSendStatusMessage() throws Exception{
		PropertiesFactoryBean pf = new PropertiesFactoryBean();
		pf.setLocation(new ClassPathResource("sample.properties"));
		pf.afterPropertiesSet();
		Properties prop =  pf.getObject();
		TwitterTemplate template = new TwitterTemplate(prop.getProperty("z_oleg.oauth.consumerKey"),
										               prop.getProperty("z_oleg.oauth.consumerSecret"),
										               prop.getProperty("z_oleg.oauth.accessToken"),
										               prop.getProperty("z_oleg.oauth.accessTokenSecret"));
		Message<?> message1 = MessageBuilder.withPayload("Polishing #springintegration migration to Spring Social. test").build();
		StatusUpdatingMessageHandler handler = new StatusUpdatingMessageHandler(template);
		handler.afterPropertiesSet();
		handler.handleMessage(message1);
	}

	@Test
	public void testStatusUpdatingMessageHandler() {
		TimelineOperations timelineOperations = Mockito.mock(TimelineOperations.class);
		Mockito.when(this.twitter.timelineOperations()).thenReturn(timelineOperations);

		ArgumentCaptor<TweetData> argument = ArgumentCaptor.forClass(TweetData.class);

		this.in1.send(new GenericMessage<String>("foo"));

		Mockito.verify(timelineOperations).updateStatus(argument.capture());
		assertEquals("foo", argument.getValue().toRequestParameters().getFirst("status"));

		Mockito.reset(timelineOperations);

		ClassPathResource media = new ClassPathResource("log4j.properties");
		this.in2.send(MessageBuilder.withPayload(Collections.singletonMap("foo", "bar"))
				.setHeader("media", media)
				.build());

		Mockito.verify(timelineOperations).updateStatus(argument.capture());
		MultiValueMap<String, Object> requestParameters = argument.getValue().toRequestParameters();
		assertEquals("bar", requestParameters.getFirst("status"));
		assertEquals(media, requestParameters.getFirst("media"));


	}

}
