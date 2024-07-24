/*
 * Copyright 2015-2024 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
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
