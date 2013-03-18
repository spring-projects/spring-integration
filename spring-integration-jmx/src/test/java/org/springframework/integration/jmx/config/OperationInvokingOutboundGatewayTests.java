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

package org.springframework.integration.jmx.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jmx.OperationInvokingMessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
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
	private MessageChannel jmxOutboundGatewayInsideChain;

	@Autowired
	private TestBean testBean;

	@Autowired
	@Qualifier("operationInvokingWithinChain.handler")
	private MessageHandler operationInvokingWithinChain;



	private static volatile int adviceCalled;

	@After
	public void resetLists() {
		testBean.messages.clear();
	}

	@Test
	public void gatewayWithReplyChannel() throws Exception {
		withReplyChannel.send(new GenericMessage<String>("1"));
		assertEquals(1, ((List<?>) withReplyChannelOutput.receive().getPayload()).size());
		withReplyChannel.send(new GenericMessage<String>("2"));
		assertEquals(2, ((List<?>) withReplyChannelOutput.receive().getPayload()).size());
		withReplyChannel.send(new GenericMessage<String>("3"));
		assertEquals(3, ((List<?>) withReplyChannelOutput.receive().getPayload()).size());
		assertEquals(3, adviceCalled);
	}

	@Test
	public void gatewayWithNoReplyChannel() throws Exception {
		withNoReplyChannel.send(new GenericMessage<String>("1"));
		assertEquals(1, testBean.messages.size());
		withNoReplyChannel.send(new GenericMessage<String>("2"));
		assertEquals(2, testBean.messages.size());
		withNoReplyChannel.send(new GenericMessage<String>("3"));
		assertEquals(3, testBean.messages.size());
	}

	@Test //INT-1029, INT-2822
	public void testOutboundGatewayInsideChain() throws Exception {
		List handlers = TestUtils.getPropertyValue(this.operationInvokingWithinChain, "handlers", List.class);
		assertEquals(1, handlers.size());
		Object handler = handlers.get(0);
		assertTrue(handler instanceof OperationInvokingMessageHandler);
		assertTrue(TestUtils.getPropertyValue(handler, "requiresReply", Boolean.class));

		jmxOutboundGatewayInsideChain.send(MessageBuilder.withPayload("1").build());
		assertEquals(1, ((List<?>) withReplyChannelOutput.receive().getPayload()).size());
		jmxOutboundGatewayInsideChain.send(MessageBuilder.withPayload("2").build());
		assertEquals(2, ((List<?>) withReplyChannelOutput.receive().getPayload()).size());
		jmxOutboundGatewayInsideChain.send(MessageBuilder.withPayload("3").build());
		assertEquals(3, ((List<?>) withReplyChannelOutput.receive().getPayload()).size());
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}

	}
}
