/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.twitter.ignored;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.twitter.core.TwitterHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.util.StringUtils;

/**
 * @author Josh Long
 * @author Oleg Zhurakouksy
 * @author Artem Bilan
 */
@ContextConfiguration
public class TestSendingDMsUsingNamespace extends AbstractJUnit4SpringContextTests {

	@Autowired
	@Qualifier("inputChannel")

	private MessageChannel inputChannel;
	@Autowired
	@Qualifier("dmOutboundWithinChain")
	private MessageChannel dmOutboundWithinChain;

	@Test
 	@Ignore
	public void testSendigRealDirectMessage() throws Throwable {
		String dmUsr = "z_oleg";
		MessageBuilder<String> mb = MessageBuilder.withPayload("'Hello world!', from the Spring Integration outbound Twitter adapter " 
				+ System.currentTimeMillis());

		if (StringUtils.hasText(dmUsr)) {
			mb.setHeader(TwitterHeaders.DM_TARGET_USER_ID, dmUsr);
		}
		inputChannel.send(mb.build());
	}

	@Test
	@Ignore
	public void testSendigDirectMessageFromChain() throws Throwable {
		String dmUsr = "z_oleg";
		MessageBuilder<String> mb = MessageBuilder.withPayload("Hello world!");

		if (StringUtils.hasText(dmUsr)) {
			mb.setHeader(TwitterHeaders.DM_TARGET_USER_ID, dmUsr);
		}
		dmOutboundWithinChain.send(mb.build());
	}

}
