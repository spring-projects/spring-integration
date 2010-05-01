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
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.message.MessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class HeaderEnrichedGatewayTests {
	@Autowired
	@Qualifier("gateway")
	private SampleGateway gateway;
	@Autowired
	@Qualifier("input")
	private DirectChannel input;
	
	private Object testPayload;

	@Test
	public void validateStaticHeaderMappings() throws Exception {
		MessageHandler handler = Mockito.mock(MessageHandler.class);	
		input.subscribe(handler);
		
		this.prepareHandlerForTest(handler);
		testPayload = "hello";
		gateway.sendString((String) testPayload);
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
		
		this.prepareHandlerForTest(handler);
		testPayload = 123;
		gateway.sendInteger((Integer) testPayload);
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
		
		this.prepareHandlerForTest(handler);
		testPayload = "withAnnotatedHeaders";
		gateway.sendStringWithParameterHeaders((String) testPayload, "headerA", "headerB");
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
	}
	@Test(expected=BeanDefinitionStoreException.class)
	public void validateFailedGatewayHeaders() throws Exception {
		new ClassPathXmlApplicationContext("HeaderEnrichedGatewayTests-failed-context.xml", HeaderEnrichedGatewayTests.class);
	}
	
	public static class Foo{
		private String bar;

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
	}
	
	/*
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void prepareHandlerForTest(MessageHandler handler){
		Mockito.reset(handler);
		Mockito.doAnswer(new Answer() {
		      public Object answer(InvocationOnMock invocation) {
		    	  Message message = (Message) invocation.getArguments()[0];
		    	  assertEquals(testPayload, message.getPayload());
		    	  assertEquals("foo", message.getHeaders().get("foo"));
		    	  assertEquals("bar", message.getHeaders().get("bar"));
		    	  assertNull(message.getHeaders().get(MessageHeaders.PREFIX + "baz"));
		    	  if (message.getPayload().equals("withAnnotatedHeaders")){
		    		  assertEquals("headerA", message.getHeaders().get("headerA"));
			    	  assertEquals("headerB", message.getHeaders().get("headerB"));
		    	  }
		          return null;
		      }})
		  .when(handler).handleMessage(Mockito.any(Message.class));
	}
	/*
	 * 
	 */
	public static interface SampleGateway {
		public void sendString(String value);

		public void sendInteger(Integer value);

		public void sendStringWithParameterHeaders(String value,
				@Header("headerA") String headerA, @Header("headerB") String headerB);
	}
}
