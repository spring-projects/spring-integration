/*
 * Copyright 2014 the original author or authors.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.twitter.outbound.TwitterSearchOutboundGateway;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class TwitterSearchOutboundGatewayParserTests {

	@Autowired
	@Qualifier("defaultTSOG.handler")
	private TwitterSearchOutboundGateway defaultTSOG;

	@Autowired
	@Qualifier("allAttsTSOG.handler")
	private TwitterSearchOutboundGateway allAttsTSOG;

	@Autowired
	private PollingConsumer polledAndAdvisedTSOG;

	@Autowired
	private Twitter twitter;

	@Test
	public void testDefault() {
		assertSame(twitter, TestUtils.getPropertyValue(defaultTSOG, "twitter"));
	}

	@Test
	public void testAllAtts() {
		assertSame(twitter, TestUtils.getPropertyValue(allAttsTSOG, "twitter"));
		assertEquals("'foo'", TestUtils.getPropertyValue(allAttsTSOG, "searchArgsExpression.expression"));
	}

	@Test
	public void testAdvised() {
		assertSame(twitter, TestUtils.getPropertyValue(polledAndAdvisedTSOG, "handler.twitter"));
		assertThat(TestUtils.getPropertyValue(polledAndAdvisedTSOG, "handler.adviceChain", ArrayList.class).get(0),
				Matchers.instanceOf(RequestHandlerRetryAdvice.class));
	}

}
