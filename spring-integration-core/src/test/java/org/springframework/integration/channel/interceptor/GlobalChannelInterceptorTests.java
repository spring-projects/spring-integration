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

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class GlobalChannelInterceptorTests {
	@SuppressWarnings("unchecked")
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
			List<SampleInterceptor> interceptoList = (List<SampleInterceptor>) iAccessor.getPropertyValue("interceptors");
			if (channelName.equals("inputA")){
				SampleInterceptor[] inter = interceptoList.toArray(new SampleInterceptor[]{});
				Assert.assertTrue(inter.length == 13);
				Assert.assertEquals("four", inter[0].getTestIdentifier());
				Assert.assertEquals("seven", inter[1].getTestIdentifier());
				Assert.assertEquals("five", inter[2].getTestIdentifier());
				Assert.assertEquals("seven", inter[3].getTestIdentifier());
				Assert.assertEquals("eight", inter[4].getTestIdentifier());
				Assert.assertEquals("seven", inter[5].getTestIdentifier());
				Assert.assertEquals("one", inter[6].getTestIdentifier());
				Assert.assertEquals("seven", inter[7].getTestIdentifier());
				Assert.assertEquals("two", inter[8].getTestIdentifier());
				Assert.assertEquals("three", inter[9].getTestIdentifier());
				Assert.assertEquals("seven", inter[10].getTestIdentifier());
				Assert.assertEquals("six", inter[11].getTestIdentifier());
				Assert.assertEquals("seven", inter[12].getTestIdentifier());
			} 
			else
			if (channelName.equals("inputB")){
				SampleInterceptor[] inter = interceptoList.toArray(new SampleInterceptor[]{});
				Assert.assertTrue(inter.length == 11);
				Assert.assertEquals("four", inter[0].getTestIdentifier());
				Assert.assertEquals("seven", inter[1].getTestIdentifier());
				Assert.assertEquals("five", inter[2].getTestIdentifier());
				Assert.assertEquals("seven", inter[3].getTestIdentifier());
				Assert.assertEquals("one", inter[4].getTestIdentifier());
				Assert.assertEquals("seven", inter[5].getTestIdentifier());
				Assert.assertEquals("two", inter[6].getTestIdentifier());
				Assert.assertEquals("three", inter[7].getTestIdentifier());
				Assert.assertEquals("seven", inter[8].getTestIdentifier());
				Assert.assertEquals("six", inter[9].getTestIdentifier());
				Assert.assertEquals("seven", inter[10].getTestIdentifier());
			} 
			else 
			if (channelName.equals("foo")){
				SampleInterceptor[] inter = interceptoList.toArray(new SampleInterceptor[]{});
				Assert.assertTrue(inter.length == 6);
				Assert.assertEquals("four", inter[0].getTestIdentifier());
				Assert.assertEquals("seven", inter[1].getTestIdentifier());
				Assert.assertEquals("five", inter[2].getTestIdentifier());
				Assert.assertEquals("seven", inter[3].getTestIdentifier());
				Assert.assertEquals("six", inter[4].getTestIdentifier());
				Assert.assertEquals("seven", inter[5].getTestIdentifier());
			}
			else 
			if (channelName.equals("bar")){
				SampleInterceptor[] inter = interceptoList.toArray(new SampleInterceptor[]{});
				Assert.assertTrue(inter.length == 2);
				Assert.assertEquals("eight", inter[0].getTestIdentifier());
				Assert.assertEquals("seven", inter[1].getTestIdentifier());
			}
			else 
			if (channelName.equals("baz")){
				SampleInterceptor[] inter = interceptoList.toArray(new SampleInterceptor[]{});
				Assert.assertTrue(inter.length == 0);
			}
		}
	}
	/**
	 * Will test mix of Ordered and un-Ordered ChannelInterceptors
	 * Individual interceptors will only be sorted within groups they are defined.
	 * For example: interceptors defined inside of channels will be sorted according to Ordered implementation
	 * If global interceptors were added BEFORE (negative order) or AFTER (ppositive order) the global stack will be sorted
	 * and added before/after the existing stack
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void validateGlobalInterceptorsOrdered(){
		ApplicationContext applicationContext = 
			new ClassPathXmlApplicationContext("GlobalChannelInterceptorTests-ordered-context.xml", GlobalChannelInterceptorTests.class);
		Map<String, AbstractMessageChannel> channels = applicationContext.getBeansOfType(AbstractMessageChannel.class);
		for (String channelName : channels.keySet()) {
			AbstractMessageChannel channel = channels.get(channelName);
			DirectFieldAccessor cAccessor = new DirectFieldAccessor(channel);
			Object iList = cAccessor.getPropertyValue("interceptors");
			DirectFieldAccessor iAccessor = new DirectFieldAccessor(iList);
			List<SampleInterceptor> interceptoList = (List<SampleInterceptor>) iAccessor.getPropertyValue("interceptors");
			if (channelName.equals("inputA")){
				SampleInterceptor[] inter = interceptoList.toArray(new SampleInterceptor[]{});
				Assert.assertTrue(inter.length == 10);
				Assert.assertEquals("ten", inter[0].getTestIdentifier());
				Assert.assertEquals("four", inter[1].getTestIdentifier());
				Assert.assertEquals("ten", inter[2].getTestIdentifier());
				Assert.assertEquals("eight", inter[3].getTestIdentifier());
				Assert.assertEquals("seven", inter[4].getTestIdentifier());
				Assert.assertEquals("five", inter[5].getTestIdentifier());
				Assert.assertEquals("ten", inter[6].getTestIdentifier());
				Assert.assertEquals("one", inter[7].getTestIdentifier());
				Assert.assertEquals("six", inter[8].getTestIdentifier());
				Assert.assertEquals("seven", inter[9].getTestIdentifier());
			} 
			
		}
	}
	@SuppressWarnings("unchecked")
	@Test
	public void validateGlobalInterceptorsUnOrdered(){
		ApplicationContext applicationContext = 
			new ClassPathXmlApplicationContext("GlobalChannelInterceptorTests-unordered-context.xml", GlobalChannelInterceptorTests.class);
		Map<String, AbstractMessageChannel> channels = applicationContext.getBeansOfType(AbstractMessageChannel.class);
		for (String channelName : channels.keySet()) {
			AbstractMessageChannel channel = channels.get(channelName);
			DirectFieldAccessor cAccessor = new DirectFieldAccessor(channel);
			Object iList = cAccessor.getPropertyValue("interceptors");
			DirectFieldAccessor iAccessor = new DirectFieldAccessor(iList);
			List<SampleInterceptor> interceptoList = (List<SampleInterceptor>) iAccessor.getPropertyValue("interceptors");
			if (channelName.equals("inputA")){
				SampleInterceptor[] inter = interceptoList.toArray(new SampleInterceptor[]{});
				Assert.assertTrue(inter.length == 7);
				Assert.assertEquals("eight", inter[0].getTestIdentifier());
				Assert.assertEquals("seven", inter[1].getTestIdentifier());
				Assert.assertEquals("five", inter[2].getTestIdentifier());
				Assert.assertEquals("six", inter[3].getTestIdentifier());
				Assert.assertEquals("seven", inter[4].getTestIdentifier());
				Assert.assertEquals("seven", inter[5].getTestIdentifier());
				Assert.assertEquals("one", inter[6].getTestIdentifier());
			} 
		}
	}
	@SuppressWarnings("unchecked")
	@Test
	public void validateGlobalInterceptorsAllPattern(){
		ApplicationContext applicationContext = 
			new ClassPathXmlApplicationContext("GlobalChannelInterceptorTests-all-context.xml", GlobalChannelInterceptorTests.class);
		Map<String, AbstractMessageChannel> channels = applicationContext.getBeansOfType(AbstractMessageChannel.class);
		for (String channelName : channels.keySet()) {
			AbstractMessageChannel channel = channels.get(channelName);
			DirectFieldAccessor cAccessor = new DirectFieldAccessor(channel);
			Object iList = cAccessor.getPropertyValue("interceptors");
			DirectFieldAccessor iAccessor = new DirectFieldAccessor(iList);
			List<SampleInterceptor> interceptoList = (List<SampleInterceptor>) iAccessor.getPropertyValue("interceptors");
			if (channelName.equals("inputA")){
				SampleInterceptor[] inter = interceptoList.toArray(new SampleInterceptor[]{});
				Assert.assertTrue(inter.length == 2);
			} else if (channelName.equals("inputB")){
				SampleInterceptor[] inter = interceptoList.toArray(new SampleInterceptor[]{});
				Assert.assertTrue(inter.length == 1);
			} else if (channelName.equals("inputC")){
				SampleInterceptor[] inter = interceptoList.toArray(new SampleInterceptor[]{});
				Assert.assertTrue(inter.length == 1);
			}
		}
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
