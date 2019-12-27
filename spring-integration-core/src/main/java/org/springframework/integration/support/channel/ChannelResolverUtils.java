/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.support.channel;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;

/**
 * Channel resolution utilities.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.2
 *
 */
public final class ChannelResolverUtils {

	public static final String CHANNEL_RESOLVER_BEAN_NAME = "integrationChannelResolver";

	private ChannelResolverUtils() {
	}

	/**
	 * Obtain a {@link DestinationResolver} registered with the
	 * {@value CHANNEL_RESOLVER_BEAN_NAME} bean name.
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 * @return the instance of {@link DestinationResolver} bean whose name is
	 * {@value CHANNEL_RESOLVER_BEAN_NAME}.
	 */
	@SuppressWarnings("unchecked")
	public static DestinationResolver<MessageChannel> getChannelResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		if (!beanFactory.containsBean(CHANNEL_RESOLVER_BEAN_NAME)) {
			return new BeanFactoryChannelResolver(beanFactory);
		}
		return beanFactory.getBean(CHANNEL_RESOLVER_BEAN_NAME, DestinationResolver.class);
	}

}
