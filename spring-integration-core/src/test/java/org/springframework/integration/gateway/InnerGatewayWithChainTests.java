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
package org.springframework.integration.gateway;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class InnerGatewayWithChainTests {

	@Autowired
	private TestGateway testGatewayWithErrorChannelA;

	@Autowired
	private TestGateway testGatewayWithErrorChannelAA;

	@Autowired
	private TestGateway testGatewayWithNoErrorChannelAAA;

	@Autowired
	private SourcePollingChannelAdapter inboundAdapterDefaultErrorChannel;

	@Autowired
	private SourcePollingChannelAdapter inboundAdapterAssignedErrorChannel;

	@Autowired
	private SubscribableChannel errorChannel;

	@Autowired
	private SubscribableChannel assignedErrorChannel;



	@Test
	public void testExceptionHandledByMainGateway(){
		String reply = testGatewayWithErrorChannelA.echo(5);
		assertEquals("ERROR from errorChannelA", reply);
	}

	@Test
	public void testExceptionHandledByMainGatewayNoErrorChannelInChain(){
		String reply = testGatewayWithErrorChannelAA.echo(0);
		assertEquals("ERROR from errorChannelA", reply);
	}

	@Test
	public void testExceptionHandledByInnerGateway(){
		String reply = testGatewayWithErrorChannelA.echo(0);
		assertEquals("ERROR from errorChannelB", reply);
	}

	// if no error channels explicitly defined exception is rethrown
	@Test(expected=ArithmeticException.class)
	public void testGatewaysNoErrorChannel(){
		testGatewayWithNoErrorChannelAAA.echo(0);
	}

	@Test
	public void testWithSPCADefaultErrorChannel() throws Exception{
		MessageHandler handler = mock(MessageHandler.class);
		errorChannel.subscribe(handler);
		inboundAdapterDefaultErrorChannel.start();
		Thread.sleep(1000);
		inboundAdapterDefaultErrorChannel.stop();
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}

	@Test
	public void testWithSPCAAssignedErrorChannel() throws Exception{
		MessageHandler handler = mock(MessageHandler.class);
		assignedErrorChannel.subscribe(handler);
		inboundAdapterAssignedErrorChannel.start();
		Thread.sleep(1000);
		inboundAdapterAssignedErrorChannel.stop();
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}

	public static interface TestGateway{
		public String echo(int value);
	}
}
