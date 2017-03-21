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

package org.springframework.integration.mock;

import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class MockIntegration {

	public static final String MOCK_INTEGRATION_CONTEXT_BEAN_NAME = "mockIntegrationContext";

	@SuppressWarnings("unchecked")
	public static <T> MessageSource<T> mockMessageSource(T payload) {
		return (MessageSource<T>) mockMessageSource(new GenericMessage<>(payload));
	}

	public static MessageSource<?> mockMessageSource(Message<?> message) {
		MessageSource messageSource = Mockito.mock(MessageSource.class);

		given(messageSource.receive())
				.willReturn(message);

		return messageSource;
	}


	@SuppressWarnings("unchecked")
	public static <T> MessageSource<T> mockMessageSource(T payload, T... payloads) {
		List<Message<T>> messages = null;

		if (payloads != null) {
			messages = new ArrayList<>(payloads.length);
			for (T p : payloads) {
				messages.add(new GenericMessage<>(p));
			}
		}

		return (MessageSource<T>) mockMessageSource(new GenericMessage<>(payload),
				(messages != null
						? messages.toArray(new Message[messages.size()])
						: null));
	}

	public static MessageSource<?> mockMessageSource(Message<?> message, Message<?>... messages) {
		MessageSource messageSource = Mockito.mock(MessageSource.class);

		given(messageSource.receive())
				.willReturn(message, messages);

		return messageSource;
	}

	private MockIntegration() {
	}

	public static class Context implements BeanFactoryAware {

		private final Map<String, Object> beans = new HashMap<>();

		private ConfigurableListableBeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			Assert.isAssignable(ConfigurableListableBeanFactory.class, beanFactory.getClass(),
					"a ConfigurableListableBeanFactory is required");
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}

		public void resetMocks() {
			this.beans.forEach((key, value) -> {
				Object endpoint = this.beanFactory.getBean(key);
				DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(endpoint);
				if (endpoint instanceof SourcePollingChannelAdapter) {
					directFieldAccessor.setPropertyValue("source", value);
				}
			});
		}

		public void instead(String pollingAdapterId, MessageSource<?> mockMessageSource) {
			instead(pollingAdapterId, mockMessageSource, true);
		}

		public void instead(String pollingAdapterId, MessageSource<?> mockMessageSource, boolean autoStartup) {
			instead(pollingAdapterId, mockMessageSource, SourcePollingChannelAdapter.class, "source", autoStartup);
		}

		private void instead(String endpointId, Object mock, Class<?> endpointClass, String property,
				boolean autoStartup) {
			Object endpoint = this.beanFactory.getBean(endpointId, endpointClass);
			DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(endpoint);
			this.beans.put(endpointId, directFieldAccessor.getPropertyValue(property));
			directFieldAccessor.setPropertyValue("source", mock);
			if (autoStartup && endpoint instanceof Lifecycle) {
				((Lifecycle) endpoint).start();
			}
		}

	}

}
