/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.PollableChannelAdapter;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.StringMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
public class ChannelAdapterParserTests extends AbstractJUnit4SpringContextTests {

	@Test
	public void targetOnly() {
		Object bean = this.applicationContext.getBean("targetOnly");
		assertTrue(bean instanceof MessageChannel);
		assertTrue(bean instanceof PollableChannel);
		assertTrue(bean instanceof PollableChannelAdapter);
		MessageChannel channel = (MessageChannel) bean;
		assertTrue(channel.send(new StringMessage("test")));
	}

	@Test
	public void targetWithDatatypeAccepts() {
		MessageChannel channel = (MessageChannel) this.applicationContext.getBean("targetWithDatatype");
		assertTrue(channel.send(new StringMessage("test")));
	}

	@Test(expected = MessageDeliveryException.class)
	public void targetWithDatatypeRejects() {
		MessageChannel channel = (MessageChannel) this.applicationContext.getBean("targetWithDatatype");
		channel.send(new GenericMessage<Integer>(123));
	}

}
