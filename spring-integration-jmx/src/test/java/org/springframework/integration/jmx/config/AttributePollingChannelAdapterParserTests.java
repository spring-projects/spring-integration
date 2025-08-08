/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 *
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class AttributePollingChannelAdapterParserTests {

	@Autowired
	private PollableChannel channel;

	@Autowired
	private SourcePollingChannelAdapter adapter;

	@Autowired
	private TestBean testBean;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired
	@Qualifier("autoChannel.adapter")
	private SourcePollingChannelAdapter autoChannelAdapter;

	@Test
	public void pollForAttribute() throws Exception {
		this.testBean.test("foo");
		this.adapter.start();
		Message<?> result = this.channel.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("foo");
	}

	@Test
	public void testAutoChannel() {
		assertThat(TestUtils.getPropertyValue(this.autoChannelAdapter, "outputChannel")).isSameAs(this.autoChannel);
	}

}
