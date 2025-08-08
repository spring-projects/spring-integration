/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aop;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.expression.Expression;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MessagePublishingInterceptorTests {

	private DestinationResolver<MessageChannel> channelResolver;

	private final QueueChannel testChannel = new QueueChannel();

	private DefaultListableBeanFactory beanFactory;

	@BeforeEach
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
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("test-foo");
	}

	@Test
	public void demoMethodNameMappingExpressionSource() {
		Map<String, String> expressionMap = new HashMap<>();
		expressionMap.put("test", "#return");
		MethodNameMappingPublisherMetadataSource metadataSource =
				new MethodNameMappingPublisherMetadataSource(expressionMap);
		Map<String, String> channelMap = new HashMap<>();
		channelMap.put("test", "c");
		metadataSource.setChannelMap(channelMap);

		Map<String, Map<String, String>> headerExpressionMap = new HashMap<>();
		Map<String, String> headerExpressions = new HashMap<>();
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
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
		assertThat(message.getHeaders().get("bar")).isEqualTo("foo");
		assertThat(message.getHeaders().get("name")).isEqualTo("oleg");
	}

	interface TestBean {

		String test();

	}

	static class TestBeanImpl implements TestBean {

		@Override
		public String test() {
			return "foo";
		}

	}

	private static class TestPublisherMetadataSource implements PublisherMetadataSource {

		TestPublisherMetadataSource() {
			super();
		}

		@Override
		public Expression getExpressionForPayload(Method method) {
			return EXPRESSION_PARSER.parseExpression("'test-' + #return");
		}

		@Override
		public Map<String, Expression> getExpressionsForHeaders(Method method) {
			return null;
		}

		@Override
		public String getChannelName(Method method) {
			return "c";
		}

	}

}
