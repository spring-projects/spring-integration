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

package org.springframework.integration.security.channel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.security.config.ChannelSecurityInterceptorBeanPostProcessor;
import org.springframework.messaging.MessageChannel;

/**
 * @author Mark Fisher
 */
public class ChannelSecurityInterceptorBeanPostProcessorTests {

	@Test
	public void securedChannelIsProxied() throws Exception {
		ChannelSecurityMetadataSource securityMetadataSource = new ChannelSecurityMetadataSource();
		securityMetadataSource.addPatternMapping(Pattern.compile("secured.*"), new DefaultChannelAccessPolicy("ROLE_ADMIN", null));

		final ChannelSecurityInterceptor interceptor = new ChannelSecurityInterceptor(securityMetadataSource);

		ListableBeanFactory beanFactory = Mockito.mock(ListableBeanFactory.class);
		Mockito.doAnswer(new Answer<Map<String, ChannelSecurityInterceptor>>() {

			@Override
			public Map<String, ChannelSecurityInterceptor> answer(InvocationOnMock invocation) throws Throwable {
				return Collections.singletonMap("interceptor", interceptor);
			}
		}).when(beanFactory).getBeansOfType(ChannelSecurityInterceptor.class);

		ChannelSecurityInterceptorBeanPostProcessor postProcessor = new ChannelSecurityInterceptorBeanPostProcessor();
		postProcessor.setBeanFactory(beanFactory);
		postProcessor.afterPropertiesSet();

		QueueChannel securedChannel = new QueueChannel();
		securedChannel.setBeanName("securedChannel");
		MessageChannel postProcessedChannel = (MessageChannel) postProcessor.postProcessAfterInitialization(securedChannel, "securedChannel");
		assertTrue(AopUtils.isAopProxy(postProcessedChannel));
	}

	@Test
	public void nonsecuredChannelIsNotProxied() throws Exception {
		ChannelSecurityMetadataSource securityMetadataSource = new ChannelSecurityMetadataSource();
		securityMetadataSource.addPatternMapping(Pattern.compile("secured.*"), new DefaultChannelAccessPolicy("ROLE_ADMIN", null));
		final ChannelSecurityInterceptor interceptor = new ChannelSecurityInterceptor(securityMetadataSource);

		ListableBeanFactory beanFactory = Mockito.mock(ListableBeanFactory.class);
		Mockito.doAnswer(new Answer<Map<String, ChannelSecurityInterceptor>>() {

			@Override
			public Map<String, ChannelSecurityInterceptor> answer(InvocationOnMock invocation) throws Throwable {
				return Collections.singletonMap("interceptor", interceptor);
			}
		}).when(beanFactory).getBeansOfType(ChannelSecurityInterceptor.class);

		ChannelSecurityInterceptorBeanPostProcessor postProcessor = new ChannelSecurityInterceptorBeanPostProcessor();
		postProcessor.setBeanFactory(beanFactory);
		postProcessor.afterPropertiesSet();

		QueueChannel channel = new QueueChannel();
		channel.setBeanName("testChannel");
		MessageChannel postProcessedChannel = (MessageChannel) postProcessor.postProcessAfterInitialization(channel, "testChannel");
		assertFalse(AopUtils.isAopProxy(postProcessedChannel));
	}

}
