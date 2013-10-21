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

package org.springframework.integration.jmx.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jmx.JmxHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class OperationInvokingChannelAdapterParserTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private MessageChannel operationWithNonNullReturn;

	@Autowired
	private MessageChannel 	operationInvokingWithinChain;

	@Autowired
	private MessageChannel 	operationWithinChainWithNonNullReturn;

	@Autowired
	private TestBean testBean;

	private static volatile int adviceCalled;

	@After
	public void resetLists() {
		testBean.messages.clear();
	}


	@Test
	public void adapterWithDefaults() throws Exception {
		assertEquals(0, testBean.messages.size());
		input.send(new GenericMessage<String>("test1"));
		input.send(new GenericMessage<String>("test2"));
		input.send(new GenericMessage<String>("test3"));
		assertEquals(3, testBean.messages.size());
		assertEquals(3, adviceCalled);
	}

	@Test
	public void testOutboundAdapterWithNonNullReturn() throws Exception {
		try {
			operationWithNonNullReturn.send(new GenericMessage<String>("test1"));
			fail("Expect MessagingException about non-null return");
		}
		catch (Exception e) {
			assertTrue(e instanceof MessagingException);
//			TODO Add check exception's message about 'must have a void return' after <jmx:operation-invoking-channel-adapter/> refactoring
		}
	}

	@Test
	// Headers should be ignored
	public void adapterWitJmxHeaders() throws Exception {
		assertEquals(0, testBean.messages.size());
		input.send(this.createMessage("1"));
		input.send(this.createMessage("2"));
		input.send(this.createMessage("3"));
		assertEquals(3, testBean.messages.size());
	}

	@Test //INT-2275
	public void testInvokeOperationWithinChain() throws Exception {
		operationInvokingWithinChain.send(new GenericMessage<String>("test1"));
		assertEquals(1, testBean.messages.size());
	}

	@Test //INT-2275
	public void testOperationWithinChainWithNonNullReturn() throws Exception {
		try {
			operationWithinChainWithNonNullReturn.send(new GenericMessage<String>("test1"));
			fail("Expect MessagingException about non-null return");
		}
		catch (Exception e) {
			assertTrue(e instanceof MessagingException);
//			TODO Add check exception's message about 'must have a void return' after <jmx:operation-invoking-channel-adapter/> refactoring
		}
	}

	private Message<?> createMessage(String payload){
		return MessageBuilder.withPayload(payload)
			.setHeader(JmxHeaders.OBJECT_NAME, "org.springframework.integration.jmx.config:type=TestBean,name=foo")
			.setHeader(JmxHeaders.OPERATION_NAME, "blah").build();
	}

	public static class FooADvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}

	}
}
