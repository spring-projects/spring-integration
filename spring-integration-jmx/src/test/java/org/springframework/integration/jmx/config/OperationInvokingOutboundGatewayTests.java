/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jmx.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jmx.outbound.OperationInvokingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Glenn Renfro
 *
 */
@SpringJUnitConfig
@DirtiesContext
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

	@AfterEach
	public void resetLists() {
		testBean.messages.clear();
	}

	@Test
	public void gatewayWithReplyChannel() throws Exception {
		withReplyChannel.send(new GenericMessage<String>("1"));
		assertThat(((List<?>) withReplyChannelOutput.receive().getPayload()).size()).isEqualTo(1);
		withReplyChannel.send(new GenericMessage<String>("2"));
		assertThat(((List<?>) withReplyChannelOutput.receive().getPayload()).size()).isEqualTo(2);
		withReplyChannel.send(new GenericMessage<String>("3"));
		assertThat(((List<?>) withReplyChannelOutput.receive().getPayload()).size()).isEqualTo(3);
		assertThat(adviceCalled).isEqualTo(3);
	}

	@Test
	public void gatewayWithPrimitiveArgs() throws Exception {
		primitiveChannel.send(new GenericMessage<Object[]>(new Object[] {true, 0L, 1}));
		assertThat(testBean.messages.size()).isEqualTo(1);
		List<Object> argList = new ArrayList<Object>();
		argList.add(false);
		argList.add(123L);
		argList.add(42);
		primitiveChannel.send(new GenericMessage<List<Object>>(argList));
		assertThat(testBean.messages.size()).isEqualTo(2);
		Map<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("p1", true);
		argMap.put("p2", 0L);
		argMap.put("p3", 42);
		primitiveChannel.send(new GenericMessage<Map<String, Object>>(argMap));
		assertThat(testBean.messages.size()).isEqualTo(3);
		argMap.put("p2", true);
		argMap.put("p1", 0L);
		argMap.put("p3", 42);
		try {
			primitiveChannel.send(new GenericMessage<Map<String, Object>>(argMap));
			fail("Expected Exception");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(MessagingException.class);
			assertThat(e.getMessage()).contains("failed to find JMX operation");
		}
		// Args are named starting with Spring 3.2.3
		argMap = new HashMap<String, Object>();
		argMap.put("bool", true);
		argMap.put("time", 0L);
		argMap.put("foo", 42);
		primitiveChannel.send(new GenericMessage<Map<String, Object>>(argMap));
		assertThat(testBean.messages.size()).isEqualTo(4);
	}

	@Test
	public void gatewayWithNoReplyChannel() throws Exception {
		withNoReplyChannel.send(new GenericMessage<String>("1"));
		assertThat(testBean.messages.size()).isEqualTo(1);
		withNoReplyChannel.send(new GenericMessage<String>("2"));
		assertThat(testBean.messages.size()).isEqualTo(2);
		withNoReplyChannel.send(new GenericMessage<String>("3"));
		assertThat(testBean.messages.size()).isEqualTo(3);
	}

	@Test //INT-1029, INT-2822
	public void testOutboundGatewayInsideChain() throws Exception {
		List<?> handlers = TestUtils.getPropertyValue(this.operationInvokingWithinChain, "handlers");
		assertThat(handlers.size()).isEqualTo(1);
		Object handler = handlers.get(0);
		assertThat(handler instanceof OperationInvokingMessageHandler).isTrue();
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "requiresReply")).isTrue();

		jmxOutboundGatewayInsideChain.send(MessageBuilder.withPayload("1").build());
		assertThat(((List<?>) withReplyChannelOutput.receive().getPayload()).size()).isEqualTo(1);
		jmxOutboundGatewayInsideChain.send(MessageBuilder.withPayload("2").build());
		assertThat(((List<?>) withReplyChannelOutput.receive().getPayload()).size()).isEqualTo(2);
		jmxOutboundGatewayInsideChain.send(MessageBuilder.withPayload("3").build());
		assertThat(((List<?>) withReplyChannelOutput.receive().getPayload()).size()).isEqualTo(3);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

}
