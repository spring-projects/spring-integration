/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.support;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.context.SmartLifecycle;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
public class SmartLifecycleRoleControllerTests {

	@Test
	public void testOrder() {
		SmartLifecycle lc1 = mock(SmartLifecycle.class);
		when(lc1.getPhase()).thenReturn(2);
		SmartLifecycle lc2 = mock(SmartLifecycle.class);
		when(lc1.getPhase()).thenReturn(1);
		MultiValueMap<String, SmartLifecycle> map = new LinkedMultiValueMap<String, SmartLifecycle>();
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

}
