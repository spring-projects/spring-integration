/*
 * Copyright 2014-2017 the original author or authors.
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

package org.springframework.integration.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.config.annotation.InboundChannelAdapterAnnotationPostProcessor;
import org.springframework.integration.core.MessageSource;

/**
 *
 * @author Oleg Zhurakousky
 *
 */
public class MessagingAnnotationUtilsTests {

	/*
	 * Validates appropriate error message during post processing failure
	 * when AliasFor annotation is miss-configured
	 */
	@Test
	public void validateAppropriateErrorMessage() throws Exception {
		String expectedErrorMessageSuffix = "attribute 'value' and its alias 'channel' are present "
				+ "with values of [bar] and [foo], but only one is permitted.";

		Method method = MessagingAnnotationUtilsTests.class.getDeclaredMethod("messageSource");
		Annotation annotation = AnnotationUtils.findAnnotation(method, InboundChannelAdapter.class);
		InboundChannelAdapterAnnotationPostProcessor pp = new InboundChannelAdapterAnnotationPostProcessor(mock(ConfigurableListableBeanFactory.class));
		try {
			pp.shouldCreateEndpoint(method, Collections.singletonList(annotation));
			fail();
		}
		catch (IllegalArgumentException e) {
			/*
			 * The error message seen prior to bug fix:
			 * A channel name in 'value' is required when interface org.springframework.integration.annotation.InboundChannelAdapter is used on '@Bean' methods.
			 */
			assertTrue(e.getMessage().endsWith(expectedErrorMessageSuffix));
		}
	}

	@Bean
	@InboundChannelAdapter(
			value = "bar",
			channel = "foo",
			poller = @Poller(fixedDelay = "1000", maxMessagesPerPoll = "1")
	)
	public MessageSource<?> messageSource() {
		return null;
	}
}
