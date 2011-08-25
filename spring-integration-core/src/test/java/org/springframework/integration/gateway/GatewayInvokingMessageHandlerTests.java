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

package org.springframework.integration.gateway;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GatewayInvokingMessageHandlerTests {

	@Autowired
	@Qualifier("inputA")
	SubscribableChannel channel;
	
	@Autowired
	@Qualifier("simpleGateway")
	SimpleGateway gateway;
	
	@Autowired
	@Qualifier("gatewayWithError")
	SimpleGateway gatewayWithError;
	
	@Autowired
	@Qualifier("gatewayWithErrorAsync")
	SimpleGateway gatewayWithErrorAsync;

	@Autowired
	@Qualifier("gatewayWithErrorChannelAndTransformer")
	SimpleGateway gatewayWithErrorChannelAndTransformer;

	@Autowired
	@Qualifier("inputB")
	SubscribableChannel output;

	@Test
	public void validateGatewayInTheChainViaChannel() {
		output.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				Assert.assertEquals("echo:echo:echo:hello", message.getPayload());
				Assert.assertEquals("foo", message.getHeaders().get("foo"));
				Assert.assertEquals("oleg", message.getHeaders().get("name"));
			}
		});
		channel.send(new GenericMessage<String>("hello"));
	}

	@Test
	public void validateGatewayInTheChainViaAnotherGateway() {	
		output.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				Assert.assertEquals("echo:echo:echo:hello", message.getPayload());
				Assert.assertEquals("foo", message.getHeaders().get("foo"));
				Assert.assertEquals("oleg", message.getHeaders().get("name"));
			}
		});
		String result = gateway.process("hello");
		Assert.assertEquals("echo:echo:echo:hello", result);
	}
	
	@Test
	public void validateGatewayWithErrorMessageReturned() {	
		try {
			String result = gatewayWithErrorChannelAndTransformer.process("echoWithRuntimeExceptionChannel");
			Assert.assertNotNull(result);
			Assert.assertEquals("Error happened in message: echoWithRuntimeExceptionChannel", result);
		}
		catch (Exception e) {
			Assert.fail();
		}
		
		try {
			gatewayWithError.process("echoWithRuntimeExceptionChannel");
			Assert.fail();
		}
		catch (SampleRuntimeException e) {
			Assert.assertEquals("echoWithRuntimeExceptionChannel", e.getMessage());
		}
		
		try {
			gatewayWithError.process("echoWithMessagingExceptionChannel");
			Assert.fail();
		}
		catch (MessageHandlingException e) {
			Assert.assertEquals("echoWithMessagingExceptionChannel", e.getFailedMessage().getPayload());
		}
		
		try {
			String result = gatewayWithErrorChannelAndTransformer.process("echoWithMessagingExceptionChannel");
			Assert.assertNotNull(result);
			Assert.assertEquals("Error happened in message: echoWithMessagingExceptionChannel", result);
		}
		catch (Exception e) {
			Assert.fail();
		}
	}
	
	@Test
	public void validateGatewayWithErrorAsync() {	
		try {
			gatewayWithErrorAsync.process("echoWithErrorAsyncChannel");
			Assert.fail();
		}
		catch (Exception e) {
			Assert.assertEquals(SampleRuntimeException.class, e.getClass());
		}
	}
	
	@Test
	public void validateGatewayWithErrorFlowReturningMessage() {	
		try {
			Object result = gatewayWithErrorChannelAndTransformer.process("echoWithErrorAsyncChannel");
			Assert.assertEquals("Error happened in message: echoWithErrorAsyncChannel", result);
		}
		catch (Exception e) {
			Assert.fail();
		}
	}


	public static class SampleErrorTransformer {
		public Message<?> toMessage(Throwable object) throws Exception {
			MessageHandlingException ex = (MessageHandlingException) object;		
			return MessageBuilder.withPayload("Error happened in message: " + ex.getFailedMessage().getPayload()).build();
		}
		
	}


	public static interface SimpleGateway {
		public String process(String str);
	}


	public static class SimpleService {

		public String echo(String value) {
			return "echo:" + value;
		}

		public String echoWithRuntimeException(String value) {
			throw new SampleRuntimeException(value);
		}

		public String echoWithMessagingException(String value) {
			throw new MessageHandlingException(new GenericMessage<String>(value));
		}

		public String echoWithErrorAsync(String value) {
			throw new SampleRuntimeException(value);
		}
	}


	@SuppressWarnings("serial")
	public static class SampleCheckedException extends Exception {
		public SampleCheckedException(String message) {
			super(message);
		}
	}


	@SuppressWarnings("serial")
	public static class SampleRuntimeException extends RuntimeException {
		public SampleRuntimeException(String message) {
			super(message);
		}		
	}

}
