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

package org.springframework.integration.support;

import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.context.SmartLifecycle;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class SmartLifecycleRoleControllerTests {

	@Test
	public void testOrder() {
		SmartLifecycle lc1 = mock(SmartLifecycle.class);
		when(lc1.getPhase()).thenReturn(2);
		SmartLifecycle lc2 = mock(SmartLifecycle.class);
		when(lc2.getPhase()).thenReturn(1);
		MultiValueMap<String, SmartLifecycle> map = new LinkedMultiValueMap<>();
		map.add("foo", lc1);
		map.add("foo", lc2);
		SmartLifecycleRoleController controller = new SmartLifecycleRoleController(map);
		controller.startLifecyclesInRole("foo");
		controller.stopLifecyclesInRole("foo");
		InOrder inOrder = inOrder(lc1, lc2);
		inOrder.verify(lc2).start();
		inOrder.verify(lc1).start();
		inOrder.verify(lc1).stop();
		inOrder.verify(lc2).stop();
	}

	@Test
	public void allEndpointRunning() {
		SmartLifecycle lc1 = new PseudoSmartLifecycle();
		SmartLifecycle lc2 = new PseudoSmartLifecycle();
		MultiValueMap<String, SmartLifecycle> map = new LinkedMultiValueMap<>();
		map.add("foo", lc1);
		map.add("foo", lc2);
		try {
			new SmartLifecycleRoleController(map);
		}
		catch (Exception e) {
			assertThat(e.getMessage())
					.contains("Cannot add the Lifecycle 'bar' to the role 'foo' " +
							"because a Lifecycle with the name 'bar' is already present.");
		}
	}

	private static class PseudoSmartLifecycle implements SmartLifecycle, NamedComponent {

		boolean running;

		@Override
		public boolean isAutoStartup() {
			return false;
		}

		@Override
		public void stop(Runnable callback) {
			running = false;
		}

		@Override
		public void start() {
			running = true;
		}

		@Override
		public void stop() {
			running = false;
		}

		@Override
		public boolean isRunning() {
			return running;
		}

		@Override
		public int getPhase() {
			return 0;
		}

		@Override
		public String getComponentName() {
			return "bar";
		}

		@Override
		public String getComponentType() {
			return PseudoSmartLifecycle.class.getName();
		}

	}

}
