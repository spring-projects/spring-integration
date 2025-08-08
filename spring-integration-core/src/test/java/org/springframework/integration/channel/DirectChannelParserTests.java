/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class DirectChannelParserTests {

	@Test
	public void testReceivesMessageFromChannelWithSource() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"directChannelParserTests.xml", DirectChannelParserTests.class);
		Object channel = context.getBean("channel");
		assertThat(channel.getClass()).isEqualTo(DirectChannel.class);
		DirectFieldAccessor dcAccessor = new DirectFieldAccessor(((DirectChannel) channel).getDispatcher());
		assertThat(dcAccessor.getPropertyValue("loadBalancingStrategy") instanceof RoundRobinLoadBalancingStrategy)
				.isTrue();
		context.close();
	}

}
