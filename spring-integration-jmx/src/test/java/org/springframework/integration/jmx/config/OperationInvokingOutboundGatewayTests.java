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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jmx.OperationInvokingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class OperationInvokingOutboundGatewayTests {

	@Autowired
	private MessageChannel withReplyChannel;

	@Autowired
	private MessageChannel primitiveChannel;

	@Autowired
	private PollableChannel withReplyChannelOutput;

	@Autowired
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
	public void gatewayWithPrimitiveArgs() throws Exception {
		primitiveChannel.send(new GenericMessage<Object[]>(new Object[] { true, 0L, 1 }));
		assertEquals(1, testBean.messages.size());
		List<Object> argList = new ArrayList<Object>();
		argList.add(false);
		argList.add(123L);
		argList.add(42);
		primitiveChannel.send(new GenericMessage<List<Object>>(argList));
		assertEquals(2, testBean.messages.size());
		Map<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("p1", true);
		argMap.put("p2", 0L);
		argMap.put("p3", 42);
		primitiveChannel.send(new GenericMessage<Map<String, Object>>(argMap));
		assertEquals(3, testBean.messages.size());
		argMap.put("p2", true);
		argMap.put("p1", 0L);
		argMap.put("p3", 42);
		try {
			primitiveChannel.send(new GenericMessage<Map<String, Object>>(argMap));
			fail("Expected Exception");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(MessagingException.class));
			assertThat(e.getMessage(), Matchers.containsString("failed to find JMX operation"));
		}
		// Args are named starting with Spring 3.2.3
		argMap = new HashMap<String, Object>();
		argMap.put("bool", true);
		argMap.put("time", 0L);
		argMap.put("foo", 42);
		primitiveChannel.send(new GenericMessage<Map<String, Object>>(argMap));
		assertEquals(4, testBean.messages.size());
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
		List<?> handlers = TestUtils.getPropertyValue(this.operationInvokingWithinChain, "handlers", List.class);
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
