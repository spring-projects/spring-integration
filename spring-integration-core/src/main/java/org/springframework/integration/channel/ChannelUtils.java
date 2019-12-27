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

package org.springframework.integration.channel;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.support.channel.ChannelResolverUtils;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * Channel utilities.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.2
 *
 */
public final class ChannelUtils {

	public static final String MESSAGE_PUBLISHING_ERROR_HANDLER_BEAN_NAME = "integrationMessagePublishingErrorHandler";

	private ChannelUtils() {
	}

	/**
	 * Obtain an {@link ErrorHandler} registered with the
	 * {@value MESSAGE_PUBLISHING_ERROR_HANDLER_BEAN_NAME} bean name.
	 * By default resolves to the {@link org.springframework.integration.channel.MessagePublishingErrorHandler}
	 * with the {@value ChannelResolverUtils#CHANNEL_RESOLVER_BEAN_NAME}
	 * {@link org.springframework.messaging.core.DestinationResolver} bean.
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 * @return the instance of {@link ErrorHandler} bean whose name is
	 * {@value MESSAGE_PUBLISHING_ERROR_HANDLER_BEAN_NAME}.
	 */
	public static ErrorHandler getErrorHandler(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		if (!beanFactory.containsBean(MESSAGE_PUBLISHING_ERROR_HANDLER_BEAN_NAME)) {
			return new MessagePublishingErrorHandler(ChannelResolverUtils.getChannelResolver(beanFactory));
		}
		return beanFactory.getBean(MESSAGE_PUBLISHING_ERROR_HANDLER_BEAN_NAME, ErrorHandler.class);
	}

}
