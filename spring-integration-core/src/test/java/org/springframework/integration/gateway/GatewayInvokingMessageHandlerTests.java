/*
 * Copyright 2002-2009 the original author or authors.
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
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException;

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
		channel.send(new StringMessage("hello"));
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
		String result = gateway.sendRecieve("hello");
		Assert.assertEquals("echo:echo:echo:hello", result);
	}
	
	@Test
	public void validateGatewayWithErrorMessageReturned() {	
		try {
			gatewayWithError.sendRecieve("echoWithErrorMessageChannel");
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals("echoWithErrorMessageChannel", e.getMessage());
		}
		
		try {
			gatewayWithError.sendRecieve("echoWithRuntimeExceptionChannel");
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals("echoWithRuntimeExceptionChannel", e.getMessage());
		}
		
		try {
			gatewayWithError.sendRecieve("echoWithMessagingExceptionChannel");
			Assert.fail();
		} catch (MessageHandlingException e) {
			Assert.assertEquals("echoWithMessagingExceptionChannel", e.getFailedMessage().getPayload());
		}
		
		try {
			gatewayWithError.sendRecieve("echoWithCheckedExceptionChannel");
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals("echoWithCheckedExceptionChannel", e.getCause().getMessage());
		}
		
		//String result = gatewayWithError.sendRecieve("echoWithErrorMessageChannel");
		//System.out.println("Result: " + result);
		//Assert.assertEquals("echo:echo:echo:hello", result);
	}
	

	public static interface SimpleGateway {
		public String sendRecieve(String str);
	}

	public static class SimpleService {
		public String echo(String value) {
			return "echo:" + value;
		}
		public Message echoWithErrorMessage(String value) {
			return MessageBuilder.withPayload(new RuntimeException(value)).build();
		}
		public RuntimeException echoWithRuntimeException(String value) {
			return new RuntimeException(value);
		}
		public MessageHandlingException echoWithMessagingException(String value) {
			return new MessageHandlingException(new StringMessage(value));
		}
		public SampleCheckedException echoWithCheckedException(String value) {
			return new SampleCheckedException(value);
		}
		public String echoWithRuntimeExceptionThrown(String value) {
			throw new RuntimeException(value);
		}
		public String echoWithMessagingExceptionThrown(String value) {
			throw new MessageHandlingException(new StringMessage(value));
		}
	}
	
	public static class SampleCheckedException extends Exception {
		public SampleCheckedException(String message){
			super(message);
		}
	}

}
