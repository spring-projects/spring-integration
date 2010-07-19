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
package org.springframework.integration.jmx.config;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.jmx.JmxHeaders;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.StringMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class OperationInvokingOutboundGatewayTests {
	@Autowired
	@Qualifier("withReplyChannel")
	private MessageChannel withReplyChannel;
	@Autowired
	@Qualifier("withReplyChannelOutput")
	private PollableChannel withReplyChannelOutput;
	
	@Autowired
	@Qualifier("withNoReplyChannel")
	private MessageChannel withNoReplyChannel;
	@Autowired
	@Qualifier("withNoReplyChannelOutput")
	private PollableChannel withNoReplyChannelOutput;
	
	@Autowired
	private TestBean testBean;

	@After
	public void resetLists() {
		testBean.messages.clear();
	}
	
	@Test
	public void gatewayWithReplyChannel() throws Exception {
		withReplyChannel.send(new StringMessage("1"));
		assertEquals(1, ((List<?>)withReplyChannelOutput.receive().getPayload()).size());
		withReplyChannel.send(new StringMessage("2"));
		assertEquals(2, ((List<?>)withReplyChannelOutput.receive().getPayload()).size());
		withReplyChannel.send(new StringMessage("3"));
		assertEquals(3, ((List<?>)withReplyChannelOutput.receive().getPayload()).size());
	}
	
//	@Test
//	public void adapterWithoutNoDefaultsAndReturnAndReplyChannel() throws Exception {
//		noDefaultInputA.send(createMessageWithHeadersForReturnCaseAndReplyChannel("1", noDefaultOutputA));
//		assertEquals(1, ((List<?>)noDefaultOutputA.receive().getPayload()).size());
//		noDefaultInputA.send(createMessageWithHeadersForReturnCaseAndReplyChannel("2", noDefaultOutputA));
//		assertEquals(2, ((List<?>)noDefaultOutputA.receive().getPayload()).size());
//		noDefaultInputA.send(createMessageWithHeadersForReturnCaseAndReplyChannel("3", noDefaultOutputA));
//		assertEquals(3, ((List<?>)noDefaultOutputA.receive().getPayload()).size());
//	}
//	
//	@Test
//	public void adapterWithoutDefaultsAndReturn() throws Exception {
//		defaultInput.send(new StringMessage("1"));
//		assertEquals(1, ((List<?>)defaultOutput.receive().getPayload()).size());
//		defaultInput.send(new StringMessage("2"));
//		assertEquals(2, ((List<?>)defaultOutput.receive().getPayload()).size());
//		defaultInput.send(new StringMessage("3"));
//		assertEquals(3, ((List<?>)defaultOutput.receive().getPayload()).size());
//	}
}
