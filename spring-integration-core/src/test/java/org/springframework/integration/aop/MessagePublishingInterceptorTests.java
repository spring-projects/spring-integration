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

package org.springframework.integration.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class MessagePublishingInterceptorTests {

	private DestinationResolver<MessageChannel> channelResolver;

	private final QueueChannel testChannel = new QueueChannel();

	private DefaultListableBeanFactory beanFactory;

	@Before
	public void setup() {
		beanFactory = new DefaultListableBeanFactory();
		channelResolver = new BeanFactoryChannelResolver(beanFactory);
		beanFactory.registerSingleton("c", testChannel);
	}

	@Test
	public void returnValue() {
		PublisherMetadataSource metadataSource = new TestPublisherMetadataSource();
		MessagePublishingInterceptor interceptor = new MessagePublishingInterceptor(metadataSource);
		interceptor.setBeanFactory(beanFactory);
		interceptor.setChannelResolver(channelResolver);
		ProxyFactory pf = new ProxyFactory(new TestBeanImpl());
		pf.addAdvice(interceptor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testChannel.receive(0);
		assertNotNull(message);
		assertEquals("test-foo", message.getPayload());
	}

	@Test
	public void demoMethodNameMappingExpressionSource() {
		Map<String, String> expressionMap = new HashMap<String, String>();
		expressionMap.put("test", "#return");
		MethodNameMappingPublisherMetadataSource metadataSource = new MethodNameMappingPublisherMetadataSource(expressionMap);
		Map<String, String> channelMap = new HashMap<String, String>();
		channelMap.put("test", "c");
		metadataSource.setChannelMap(channelMap);

		Map<String, Map<String, String>> headerExpressionMap = new HashMap<String, Map<String, String>>();
		Map<String, String> headerExpressions = new HashMap<String, String>();
		headerExpressions.put("bar", "#return");
		headerExpressions.put("name", "'oleg'");
		headerExpressionMap.put("test", headerExpressions);
		metadataSource.setHeaderExpressionMap(headerExpressionMap);

		MessagePublishingInterceptor interceptor = new MessagePublishingInterceptor(metadataSource);
		interceptor.setBeanFactory(beanFactory);
		interceptor.setChannelResolver(channelResolver);
		ProxyFactory pf = new ProxyFactory(new TestBeanImpl());
		pf.addAdvice(interceptor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testChannel.receive(0);
		assertNotNull(message);
		assertEquals("foo", message.getPayload());
		assertEquals("foo", message.getHeaders().get("bar"));
		assertEquals("oleg", message.getHeaders().get("name"));
	}


	static interface TestBean {

		String test();

	}


	static class TestBeanImpl implements TestBean {

		public String test() {
			return "foo";
		}

	}


	private static class TestPublisherMetadataSource implements PublisherMetadataSource {

		public String getPayloadExpression(Method method) {
			return "'test-' + #return";
		}

		public Map<String, String> getHeaderExpressions(Method method) {
			return null;
		}

		public String getChannelName(Method method) {
			return "c";
		}
	}

}
