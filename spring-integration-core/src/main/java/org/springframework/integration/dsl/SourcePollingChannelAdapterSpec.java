/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.lang.Nullable;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class SourcePollingChannelAdapterSpec extends
		EndpointSpec<SourcePollingChannelAdapterSpec, SourcePollingChannelAdapterFactoryBean, MessageSource<?>> {

	protected SourcePollingChannelAdapterSpec(MessageSource<?> messageSource) {
		super(messageSource, new SourcePollingChannelAdapterFactoryBean());
		this.endpointFactoryBean.setSource(messageSource);
	}

	public SourcePollingChannelAdapterSpec phase(int phase) {
		this.endpointFactoryBean.setPhase(phase);
		return _this();
	}

	public SourcePollingChannelAdapterSpec autoStartup(boolean autoStartup) {
		this.endpointFactoryBean.setAutoStartup(autoStartup);
		return _this();
	}

	public SourcePollingChannelAdapterSpec poller(@Nullable PollerMetadata pollerMetadata) {
		if (pollerMetadata != null) {
			if (PollerMetadata.MAX_MESSAGES_UNBOUNDED == pollerMetadata.getMaxMessagesPerPoll()) {
				pollerMetadata.setMaxMessagesPerPoll(1);
			}
			this.endpointFactoryBean.setPollerMetadata(pollerMetadata);
		}
		return _this();
	}

	@Override
	public SourcePollingChannelAdapterSpec role(String role) {
		this.endpointFactoryBean.setRole(role);
		return this;
	}

	/**
	 * The timeout for blocking send on channels.
	 * @param sendTimeout the timeout to use.
	 * @return the spec.
	 * @since 6.3.9
	 */
	public SourcePollingChannelAdapterSpec sendTimeout(long sendTimeout) {
		this.endpointFactoryBean.setSendTimeout(sendTimeout);
		return this;
	}

}
