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
package org.springframework.integration.channel.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;

/**
 * @author Oleg Zhurakousky
 * @author Dave Turanski
 * @since 2.0
 */
@SuppressWarnings("all")
public class GlobalChannelInterceptorTests {
	
	@Test
	public void validateGlobalInterceptor(){
		ApplicationContext applicationContext = 
			new ClassPathXmlApplicationContext("GlobalChannelInterceptorTests-context.xml", GlobalChannelInterceptorTests.class);
		Map<String, AbstractMessageChannel> channels = applicationContext.getBeansOfType(AbstractMessageChannel.class);
		for (String channelName : channels.keySet()) {
			AbstractMessageChannel channel = channels.get(channelName);
			DirectFieldAccessor cAccessor = new DirectFieldAccessor(channel);
			Object iList = cAccessor.getPropertyValue("interceptors");
			DirectFieldAccessor iAccessor = new DirectFieldAccessor(iList);
			List<SampleInterceptor> interceptorList = (List<SampleInterceptor>) iAccessor.getPropertyValue("interceptors");
			if (channelName.equals("inputA")){ // 328741
				ChannelInterceptor[] inter = interceptorList.toArray(new ChannelInterceptor[]{});
				Assert.assertTrue(inter.length ==10);
				Assert.assertEquals("interceptor-three", inter[0].toString());
				Assert.assertEquals("interceptor-two", inter[1].toString());
				Assert.assertEquals("interceptor-eight", inter[2].toString());
				Assert.assertEquals("interceptor-seven", inter[3].toString());
				Assert.assertEquals("interceptor-five", inter[4].toString());
				Assert.assertEquals("interceptor-six", inter[5].toString());
				Assert.assertEquals("interceptor-ten", inter[6].toString());
				Assert.assertEquals("interceptor-eleven", inter[7].toString());
				Assert.assertEquals("interceptor-four", inter[8].toString());
				Assert.assertEquals("interceptor-one", inter[9].toString());
			} 
			else
			if (channelName.equals("inputB")){
				ChannelInterceptor[] inter = interceptorList.toArray(new ChannelInterceptor[]{});
				Assert.assertTrue(inter.length == 6);
				Assert.assertEquals("interceptor-three", inter[0].toString());
				Assert.assertEquals("interceptor-two", inter[1].toString());
				Assert.assertEquals("interceptor-ten", inter[2].toString());
				Assert.assertEquals("interceptor-eleven", inter[3].toString());
				Assert.assertEquals("interceptor-four", inter[4].toString());
				Assert.assertEquals("interceptor-one", inter[5].toString());
			} 
			else 
			if (channelName.equals("foo")){
				ChannelInterceptor[] inter = interceptorList.toArray(new ChannelInterceptor[]{});
				Assert.assertTrue(inter.length == 6);
				Assert.assertEquals("interceptor-two", inter[0].toString());
				Assert.assertEquals("interceptor-five", inter[1].toString());
				Assert.assertEquals("interceptor-ten", inter[2].toString());
				Assert.assertEquals("interceptor-eleven", inter[3].toString());
				Assert.assertEquals("interceptor-four", inter[4].toString());
				Assert.assertEquals("interceptor-one", inter[5].toString());
			}
			else 
			if (channelName.equals("bar")){
				ChannelInterceptor[] inter = interceptorList.toArray(new ChannelInterceptor[]{});
				Assert.assertTrue(inter.length == 4);
				Assert.assertEquals("interceptor-eight", inter[0].toString());
				Assert.assertEquals("interceptor-seven", inter[1].toString());
				Assert.assertEquals("interceptor-ten", inter[2].toString());
				Assert.assertEquals("interceptor-eleven", inter[3].toString());
			}
			else 
			if (channelName.equals("baz")){
				ChannelInterceptor[] inter = interceptorList.toArray(new ChannelInterceptor[]{});
				Assert.assertTrue(inter.length == 2);
				Assert.assertEquals("interceptor-ten", inter[0].toString());
				Assert.assertEquals("interceptor-eleven", inter[1].toString());
			}
		}
	}
	
	@Test
	public void testWildCardPatternMatch(){
		ApplicationContext applicationContext = 
			new ClassPathXmlApplicationContext("GlobalChannelInterceptorTests-context.xml", GlobalChannelInterceptorTests.class);
         AbstractMessageChannel channel = applicationContext.getBean("inpuC",AbstractMessageChannel.class);
         DirectFieldAccessor cAccessor = new DirectFieldAccessor(channel);
			Object iList = cAccessor.getPropertyValue("interceptors");
			DirectFieldAccessor iAccessor = new DirectFieldAccessor(iList);
			List<SampleInterceptor> interceptorList = (List<SampleInterceptor>) iAccessor.getPropertyValue("interceptors");
			List<String> interceptorNames = new ArrayList<String>();
			for (Object interceptor: interceptorList){
				interceptorNames.add(interceptor.toString());
			}
			Assert.assertTrue(interceptorNames.contains("interceptor-ten"));
			Assert.assertTrue(interceptorNames.contains("interceptor-eleven"));
	}

	
	public static class SampleInterceptor implements ChannelInterceptor {
		private String testIdentifier;
		public String getTestIdentifier() {
			return testIdentifier;
		}
		public void setTestIdentifier(String testIdentifier) {
			this.testIdentifier = testIdentifier;
		}
		public Message<?> postReceive(Message<?> message, MessageChannel channel) {
			return null;
		}
		public void postSend(Message<?> message, MessageChannel channel,
				boolean sent) {
		}
		public boolean preReceive(MessageChannel channel) {
			return false;
		}
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			return null;
		}	
		public String toString(){
			return "interceptor-" + testIdentifier; 
		}
		
	}
	public static class SampleOrderedInterceptor extends SampleInterceptor implements Ordered {
		private int order;
		public int getOrder() {
			return order;
		}	
		public void setOrder(int order) {
			this.order = order;
		}
	}
}
