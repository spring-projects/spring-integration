/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
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
