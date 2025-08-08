/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0.1
 */
@SpringJUnitConfig
@DirtiesContext
public class GlobalChannelInterceptorTests extends ActiveMQMultiContextTests {

	@Autowired
	InterceptableChannel jmsChannel;

	@Test
	public void testJmsChannel() {
		List<ChannelInterceptor> interceptors = this.jmsChannel.getInterceptors();
		assertThat(interceptors).isNotNull();
		assertThat(interceptors.size()).isEqualTo(1);
		assertThat(interceptors.get(0) instanceof SampleInterceptor).isTrue();
	}

	public static class SampleInterceptor implements ChannelInterceptor {

	}

}
