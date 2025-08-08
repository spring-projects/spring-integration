/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
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
