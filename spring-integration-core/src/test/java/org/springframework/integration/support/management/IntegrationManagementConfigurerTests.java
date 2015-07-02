/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.integration.support.management;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.router.RecipientListRouter;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
public class IntegrationManagementConfigurerTests {

	@Test
	public void testLogging() {
		DirectChannel channel = new DirectChannel();
		AbstractMessageHandler handler = new RecipientListRouter();
		AbstractMessageSource<?> source = new AbstractMessageSource<Object>() {

			@Override
			public String getComponentType() {
				return null;
			}

			@Override
			protected Object doReceive() {
				return null;
			}
		};
		assertTrue(channel.isLoggingEnabled());
		assertTrue(handler.isLoggingEnabled());
		assertTrue(source.isLoggingEnabled());
		ApplicationContext ctx = mock(ApplicationContext.class);
		Map<String, IntegrationManagement> beans = new HashMap<String, IntegrationManagement>();
		beans.put("foo", channel);
		beans.put("bar", handler);
		beans.put("baz", source);
		when(ctx.getBeansOfType(IntegrationManagement.class)).thenReturn(beans);
		IntegrationManagementConfigurer configurer = new IntegrationManagementConfigurer();
		configurer.setApplicationContext(ctx);
		configurer.setDefaultLoggingEnabled(false);
		configurer.afterSingletonsInstantiated();
		assertFalse(channel.isLoggingEnabled());
		assertFalse(handler.isLoggingEnabled());
		assertFalse(source.isLoggingEnabled());
	}

}
