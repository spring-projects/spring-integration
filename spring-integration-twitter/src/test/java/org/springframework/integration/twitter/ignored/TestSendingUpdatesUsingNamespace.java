/*
 * Copyright 2002-2012 the original author or authors
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

package org.springframework.integration.twitter.ignored;

import java.util.Date;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * @author Josh Long
 * @author Artem Bilan
 */
@ContextConfiguration
public class TestSendingUpdatesUsingNamespace extends AbstractJUnit4SpringContextTests {

	private MessagingTemplate messagingTemplate = new MessagingTemplate();

	@Value("#{out}") private MessageChannel channel;

	@Autowired
	@Qualifier("outFromChain")
	private MessageChannel outFromChain;

	@Test
	@Ignore
	public void testSendingATweet() throws Throwable {
		MessageBuilder<String> mb = MessageBuilder.withPayload("Early start today"
				+ new Date(System.currentTimeMillis()));
		Message<String> m = mb.build();
		this.messagingTemplate.send(this.channel, m);
	}

	@Test
	@Ignore
	public void testSendingATweetFromChain() throws Throwable {
		Message<String> m = MessageBuilder.withPayload("Early start today" + new Date(System.currentTimeMillis())).build();
		this.outFromChain.send(m);
	}

}
