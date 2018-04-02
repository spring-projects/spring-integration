/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
		assertFalse(this.in.isRunning());
		assertFalse(this.out1.isRunning());
		assertFalse(this.out2.isRunning());
		assertFalse(this.out3.isRunning());
		assertFalse(this.out4.isRunning());
		assertFalse(this.bridge.isRunning());

		this.controller.startLifecyclesInRole("cluster");

		assertTrue(this.in.isRunning());
		assertTrue(this.out1.isRunning());
		assertTrue(this.out2.isRunning());
		assertTrue(this.out3.isRunning());
		assertFalse(this.out4.isRunning());
		assertTrue(this.bridge.isRunning());

		this.controller.stopLifecyclesInRole("cluster");

		assertFalse(this.in.isRunning());
		assertFalse(this.out1.isRunning());
		assertFalse(this.out2.isRunning());
		assertFalse(this.out3.isRunning());
		assertFalse(this.out4.isRunning());
		assertFalse(this.bridge.isRunning());

		this.controller.onApplicationEvent(new OnGrantedEvent("foo", null, "cluster"));

		assertTrue(this.in.isRunning());
		assertTrue(this.out1.isRunning());
		assertTrue(this.out2.isRunning());
		assertTrue(this.out3.isRunning());
		assertFalse(this.out4.isRunning());
		assertTrue(this.bridge.isRunning());

		this.controller.onApplicationEvent(new OnRevokedEvent("foo", null, "cluster"));

		assertFalse(this.in.isRunning());
		assertFalse(this.out1.isRunning());
		assertFalse(this.out2.isRunning());
		assertFalse(this.out3.isRunning());
		assertFalse(this.out4.isRunning());
		assertFalse(this.bridge.isRunning());

		assertFalse(this.controller.allEndpointsRunning("cluster"));
	}

	public static class Sink {

		public void foo(String s) { }

	}

}
