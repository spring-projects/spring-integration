/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class EndpointRoleParserTests {

	@Autowired
	private SourcePollingChannelAdapter in;

	@Autowired
	private EventDrivenConsumer out1;

	@Autowired
	private EventDrivenConsumer out2;

	@Autowired
	private EventDrivenConsumer out3;

	@Autowired
	private EventDrivenConsumer out4;

	@Autowired
	private EventDrivenConsumer bridge;

	@Autowired
	private SmartLifecycleRoleController controller;

	@Test
	public void test() {
		assertThat(this.in.isRunning()).isFalse();
		assertThat(this.out1.isRunning()).isFalse();
		assertThat(this.out2.isRunning()).isFalse();
		assertThat(this.out3.isRunning()).isFalse();
		assertThat(this.out4.isRunning()).isFalse();
		assertThat(this.bridge.isRunning()).isFalse();

		this.controller.startLifecyclesInRole("cluster");

		assertThat(this.in.isRunning()).isTrue();
		assertThat(this.out1.isRunning()).isTrue();
		assertThat(this.out2.isRunning()).isTrue();
		assertThat(this.out3.isRunning()).isTrue();
		assertThat(this.out4.isRunning()).isFalse();
		assertThat(this.bridge.isRunning()).isTrue();

		this.controller.stopLifecyclesInRole("cluster");

		assertThat(this.in.isRunning()).isFalse();
		assertThat(this.out1.isRunning()).isFalse();
		assertThat(this.out2.isRunning()).isFalse();
		assertThat(this.out3.isRunning()).isFalse();
		assertThat(this.out4.isRunning()).isFalse();
		assertThat(this.bridge.isRunning()).isFalse();

		this.controller.onApplicationEvent(new OnGrantedEvent("foo", null, "cluster"));

		assertThat(this.in.isRunning()).isTrue();
		assertThat(this.out1.isRunning()).isTrue();
		assertThat(this.out2.isRunning()).isTrue();
		assertThat(this.out3.isRunning()).isTrue();
		assertThat(this.out4.isRunning()).isFalse();
		assertThat(this.bridge.isRunning()).isTrue();

		this.controller.onApplicationEvent(new OnRevokedEvent("foo", null, "cluster"));

		assertThat(this.in.isRunning()).isFalse();
		assertThat(this.out1.isRunning()).isFalse();
		assertThat(this.out2.isRunning()).isFalse();
		assertThat(this.out3.isRunning()).isFalse();
		assertThat(this.out4.isRunning()).isFalse();
		assertThat(this.bridge.isRunning()).isFalse();

		assertThat(this.controller.allEndpointsRunning("cluster")).isFalse();
	}

	public static class Sink {

		public void foo(String s) {
		}

	}

}
