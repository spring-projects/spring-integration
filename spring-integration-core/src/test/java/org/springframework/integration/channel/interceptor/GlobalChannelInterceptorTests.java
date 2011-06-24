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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GlobalChannelInterceptorTests {
	@Autowired
	ApplicationContext applicationContext;
	
	@Test
	public void validateGlobalInterceptor() throws Exception{
 		Map<String, MessageChannel> channels = applicationContext.getBeansOfType(MessageChannel.class);
		for (String channelName : channels.keySet()) {
			MessageChannel channel = channels.get(channelName);
			if (channelName.equals("nullChannel")){
				continue;
			}
			if (AopUtils.isAopProxy(channel)){
				channel = (MessageChannel) ((Advised)channel).getTargetSource().getTarget();
			}
			List<?> interceptorList = TestUtils.getPropertyValue(channel, "interceptors.interceptors", List.class);
			ChannelInterceptor[] interceptors = interceptorList.toArray(new ChannelInterceptor[] {}); 
			if (channelName.equals("inputA")){ // 328741
				Assert.assertTrue(interceptors.length ==10);
				Assert.assertEquals("interceptor-three", interceptors[0].toString());
				Assert.assertEquals("interceptor-two", interceptors[1].toString());
				Assert.assertEquals("interceptor-eight", interceptors[2].toString());
				Assert.assertEquals("interceptor-seven", interceptors[3].toString());
				Assert.assertEquals("interceptor-five", interceptors[4].toString());
				Assert.assertEquals("interceptor-six", interceptors[5].toString());
				Assert.assertEquals("interceptor-ten", interceptors[6].toString());
				Assert.assertEquals("interceptor-eleven", interceptors[7].toString());
				Assert.assertEquals("interceptor-four", interceptors[8].toString());
				Assert.assertEquals("interceptor-one", interceptors[9].toString());
			} 
			else if (channelName.equals("inputB")) {
				Assert.assertTrue(interceptors.length == 6);
				Assert.assertEquals("interceptor-three", interceptors[0].toString());
				Assert.assertEquals("interceptor-two", interceptors[1].toString());
				Assert.assertEquals("interceptor-ten", interceptors[2].toString());
				Assert.assertEquals("interceptor-eleven", interceptors[3].toString());
				Assert.assertEquals("interceptor-four", interceptors[4].toString());
				Assert.assertEquals("interceptor-one", interceptors[5].toString());
			} 
			else if (channelName.equals("foo")) {
				Assert.assertTrue(interceptors.length == 6);
				Assert.assertEquals("interceptor-two", interceptors[0].toString());
				Assert.assertEquals("interceptor-five", interceptors[1].toString());
				Assert.assertEquals("interceptor-ten", interceptors[2].toString());
				Assert.assertEquals("interceptor-eleven", interceptors[3].toString());
				Assert.assertEquals("interceptor-four", interceptors[4].toString());
				Assert.assertEquals("interceptor-one", interceptors[5].toString());
			}
			else if (channelName.equals("bar")) {
				Assert.assertTrue(interceptors.length == 4);
				Assert.assertEquals("interceptor-eight", interceptors[0].toString());
				Assert.assertEquals("interceptor-seven", interceptors[1].toString());
				Assert.assertEquals("interceptor-ten", interceptors[2].toString());
				Assert.assertEquals("interceptor-eleven", interceptors[3].toString());
			}
			else if (channelName.equals("baz")) {
				Assert.assertTrue(interceptors.length == 2);
				Assert.assertEquals("interceptor-ten", interceptors[0].toString());
				Assert.assertEquals("interceptor-eleven", interceptors[1].toString());
			}
			else if (channelName.equals("inputWithProxy")) {
				Assert.assertTrue(interceptors.length == 6);
			}
		}
	}

	
	@Autowired
	@Qualifier("inpuC")
	MessageChannel inpuCchannel;
	@Test
	public void testWildCardPatternMatch() {
		 
		List<?> interceptorList = TestUtils.getPropertyValue(inpuCchannel, "interceptors.interceptors", List.class);
		List<String> interceptorNames = new ArrayList<String>();
		for (Object interceptor : interceptorList) {
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

		public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
		}

		public boolean preReceive(MessageChannel channel) {
			return false;
		}

		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			return null;
		}	

		public String toString() {
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
	
	public static class TestInterceptor implements MethodInterceptor{

		public Object invoke(MethodInvocation invocation) throws Throwable {
			return invocation.proceed();
		}
		
	}

}
