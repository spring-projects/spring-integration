/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.channel.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.ThreadLocalChannel;
import org.springframework.integration.config.TestChannelInterceptor;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ThreadLocalChannelParserTests {

	@Autowired @Qualifier("simpleChannel")
	private MessageChannel simpleChannel;

	@Autowired @Qualifier("channelWithInterceptor")
	private MessageChannel channelWithInterceptor;

	@Autowired
	private TestChannelInterceptor interceptor;


	@Test
	public void checkType() {
		assertEquals(ThreadLocalChannel.class, simpleChannel.getClass());
	}

	@Test
	public void verifyInterceptor() {
		assertEquals(0, interceptor.getSendCount());
		channelWithInterceptor.send(new StringMessage("test"));
		assertEquals(1, interceptor.getSendCount());
	}

}
