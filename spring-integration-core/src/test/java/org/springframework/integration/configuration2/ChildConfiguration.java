/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.configuration2;

/**
 * @author Artem Bilan
 * @since 4.0
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.MessageChannel;

@Configuration
@EnableIntegration
public class ChildConfiguration {

	@Bean
	public MessageChannel foo() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel bar() {
		return new DirectChannel();
	}

	@Bean
	@GlobalChannelInterceptor(patterns = "*")
	public WireTap baz() {
		return new WireTap(this.bar());
	}

}
