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
	@Qualifier("noDefaultInput")
	private MessageChannel noDefaultInput;
	
	@Autowired
	@Qualifier("noDefaultOutput")
	private PollableChannel noDefaultOutput;
	@Autowired
	@Qualifier("noDefaultInputA")
	private MessageChannel noDefaultInputA;
	
	@Autowired
	@Qualifier("noDefaultOutputA")
	private PollableChannel noDefaultOutputA;
	
	@Autowired
	@Qualifier("defaultInput")
	private MessageChannel defaultInput;
	
	@Autowired
	@Qualifier("defaultOutput")
	private PollableChannel defaultOutput;
	
	@Autowired
	private TestBean testBeanForDefaultsGateway;

	@Autowired
	private TestBean testBeanForNoDefaultsGateway;
	@After
	public void resetLists() {
		testBeanForDefaultsGateway.messages.clear();
		testBeanForNoDefaultsGateway.messages.clear();
	}
	
	@Test
	public void adapterWithoutNoDefaultsAndReturn() throws Exception {
		noDefaultInput.send(createMessageWithHeadersForReturnCase("1"));
		assertEquals(1, ((List<?>)noDefaultOutput.receive().getPayload()).size());
		noDefaultInput.send(createMessageWithHeadersForReturnCase("2"));
		assertEquals(2, ((List<?>)noDefaultOutput.receive().getPayload()).size());
		noDefaultInput.send(createMessageWithHeadersForReturnCase("3"));
		assertEquals(3, ((List<?>)noDefaultOutput.receive().getPayload()).size());
	}
	
	@Test
	public void adapterWithoutNoDefaultsAndReturnAndReplyChannel() throws Exception {
		noDefaultInputA.send(createMessageWithHeadersForReturnCaseAndReplyChannel("1", noDefaultOutputA));
		assertEquals(1, ((List<?>)noDefaultOutputA.receive().getPayload()).size());
		noDefaultInputA.send(createMessageWithHeadersForReturnCaseAndReplyChannel("2", noDefaultOutputA));
		assertEquals(2, ((List<?>)noDefaultOutputA.receive().getPayload()).size());
		noDefaultInputA.send(createMessageWithHeadersForReturnCaseAndReplyChannel("3", noDefaultOutputA));
		assertEquals(3, ((List<?>)noDefaultOutputA.receive().getPayload()).size());
	}
	
	@Test
	public void adapterWithoutDefaultsAndReturn() throws Exception {
		defaultInput.send(new StringMessage("1"));
		assertEquals(1, ((List<?>)defaultOutput.receive().getPayload()).size());
		defaultInput.send(new StringMessage("2"));
		assertEquals(2, ((List<?>)defaultOutput.receive().getPayload()).size());
		defaultInput.send(new StringMessage("3"));
		assertEquals(3, ((List<?>)defaultOutput.receive().getPayload()).size());
	}
	
	private static Message<String> createMessageWithHeadersForReturnCase(String payload) {
		String objectName = "org.springframework.integration.jmx.config:name=testBeanForNoDefaultsGateway,type=TestBean";
		return MessageBuilder.withPayload(payload)
				.setHeader(JmxHeaders.OBJECT_NAME, objectName)
				.setHeader(JmxHeaders.OPERATION_NAME, "testWithReturn")
				.build();
	}
	
	private static Message<String> createMessageWithHeadersForReturnCaseAndReplyChannel(String payload, MessageChannel replyChannel) {
		String objectName = "org.springframework.integration.jmx.config:name=testBeanForNoDefaultsGateway,type=TestBean";
		return MessageBuilder.withPayload(payload)
				.setHeader(JmxHeaders.OBJECT_NAME, objectName)
				.setHeader(JmxHeaders.OPERATION_NAME, "testWithReturn")
				.setReplyChannel(replyChannel)
				.build();
	}
}
