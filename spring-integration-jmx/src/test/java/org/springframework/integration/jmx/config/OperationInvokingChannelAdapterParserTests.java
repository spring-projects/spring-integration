/*
 * Copyright 2002-2019 the original author or authors.
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
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.apache.commons.logging.Log;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jmx.JmxHeaders;
import org.springframework.integration.jmx.OperationInvokingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
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

	@Autowired
	private BeanFactory beanFactory;

	@Autowired
	@Qualifier("operationWithNonNullReturn.handler")
	private OperationInvokingMessageHandler operationWithNonNullReturnHandler;

	@Autowired
	@Qualifier("chainWithOperation$child.operationWithinChainWithNonNullReturnHandler.handler")
	private OperationInvokingMessageHandler operationWithinChainWithNonNullReturnHandler;

	private static volatile int adviceCalled;

	@After
	public void resetLists() {
		testBean.messages.clear();
	}


	@Test
	public void adapterWithDefaults() {
		assertEquals(0, testBean.messages.size());
		input.send(new GenericMessage<>("test1"));
		input.send(new GenericMessage<>("test2"));
		input.send(new GenericMessage<>("test3"));
		assertEquals(3, testBean.messages.size());
		assertEquals(3, adviceCalled);
	}

	@Test
	public void testOutboundAdapterWithNonNullReturn() {
		Log logger = spy(TestUtils.getPropertyValue(this.operationWithNonNullReturnHandler, "logger", Log.class));

		willReturn(true)
				.given(logger)
				.isWarnEnabled();

		new DirectFieldAccessor(this.operationWithNonNullReturnHandler)
				.setPropertyValue("logger", logger);

		this.operationWithNonNullReturn.send(new GenericMessage<>("test1"));

		verify(logger).warn("This component doesn't expect a reply. " +
				"The MBean operation 'testWithReturn' result '[test1]' for " +
				"'org.springframework.integration.jmx.config:type=TestBean,name=testBeanAdapter' is ignored.");
	}

	@Test
	// Headers should be ignored
	public void adapterWitJmxHeaders() {
		assertEquals(0, testBean.messages.size());
		input.send(this.createMessage("1"));
		input.send(this.createMessage("2"));
		input.send(this.createMessage("3"));
		assertEquals(3, testBean.messages.size());
	}

	@Test //INT-2275
	public void testInvokeOperationWithinChain() {
		operationInvokingWithinChain.send(new GenericMessage<>("test1"));
		assertEquals(1, testBean.messages.size());
	}

	@Test
	public void testOperationWithinChainWithNonNullReturn() {
		Log logger =
				spy(TestUtils.getPropertyValue(this.operationWithinChainWithNonNullReturnHandler, "logger", Log.class));

		willReturn(true)
				.given(logger)
				.isWarnEnabled();

		new DirectFieldAccessor(this.operationWithinChainWithNonNullReturnHandler)
				.setPropertyValue("logger", logger);
			this.operationWithinChainWithNonNullReturn.send(new GenericMessage<>("test1"));
		verify(logger).warn("This component doesn't expect a reply. " +
				"The MBean operation 'testWithReturn' result '[test1]' for " +
				"'org.springframework.integration.jmx.config:type=TestBean,name=testBeanAdapter' is ignored.");
	}

	private Message<?> createMessage(String payload) {
		return MessageBuilder.withPayload(payload)
			.setHeader(JmxHeaders.OBJECT_NAME, "org.springframework.integration.jmx.config:type=TestBean,name=foo")
			.setHeader(JmxHeaders.OPERATION_NAME, "blah").build();
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}

	}
}
