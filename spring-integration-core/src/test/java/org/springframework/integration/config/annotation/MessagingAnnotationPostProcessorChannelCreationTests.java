/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.config.annotation;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.core.DestinationResolutionException;

/**
 * @author Gary Russell
 * @since 4.3.8
 *
 */
public class MessagingAnnotationPostProcessorChannelCreationTests {

	@Test
	public void testAutoCreateChannel() {
		ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
		given(beanFactory.getBean("channel", MessageChannel.class)).willThrow(NoSuchBeanDefinitionException.class);
		willAnswer(invocation -> invocation.getArgument(0))
				.given(beanFactory).initializeBean(any(DirectChannel.class), eq("channel"));
		willAnswer(invocation -> invocation.getArgument(0))
				.given(beanFactory).initializeBean(any(MessageHandler.class), eq("foo.foo.serviceActivator.handler"));
		MessagingAnnotationPostProcessor mapp = new MessagingAnnotationPostProcessor();
		mapp.setBeanFactory(beanFactory);
		mapp.afterPropertiesSet();
		mapp.postProcessAfterInitialization(new Foo(), "foo");
		verify(beanFactory).registerSingleton(eq("channel"), any(DirectChannel.class));
	}

	@Test
	public void testDontCreateChannelWhenChannelHasBadDefinition() {
		ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
		given(beanFactory.getBean("channel", MessageChannel.class)).willThrow(BeanCreationException.class);
		willAnswer(invocation -> invocation.getArgument(0))
				.given(beanFactory).initializeBean(any(DirectChannel.class), eq("channel"));
		willAnswer(invocation -> invocation.getArgument(0))
				.given(beanFactory).initializeBean(any(MessageHandler.class), eq("foo.foo.serviceActivator.handler"));
		MessagingAnnotationPostProcessor mapp = new MessagingAnnotationPostProcessor();
		mapp.setBeanFactory(beanFactory);
		mapp.afterPropertiesSet();
		try {
			mapp.postProcessAfterInitialization(new Foo(), "foo");
			fail("Expected a DestinationResolutionException");
		}
		catch (DestinationResolutionException e) {
			assertThat(e.getMessage(),
					containsString("A bean definition with name 'channel' exists, but failed to be created"));
		}
	}

	public static class Foo {

		@ServiceActivator(inputChannel = "channel")
		public void foo(String in) {
			// empty
		}

	}

}
