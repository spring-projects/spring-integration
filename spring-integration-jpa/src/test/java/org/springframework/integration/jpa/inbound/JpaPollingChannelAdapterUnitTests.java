/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.jpa.inbound;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.integration.jpa.core.JpaExecutor;

/**
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public class JpaPollingChannelAdapterUnitTests {

	/**
	 *
	 */
	@Test
	public void testReceiveNull() {

		JpaExecutor jpaExecutor = mock(JpaExecutor.class);

		when(jpaExecutor.poll()).thenReturn(null);

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);
		assertNull(jpaPollingChannelAdapter.receive());

	}

	/**
	*
	*/
	@Test
	public void testReceiveNotNull() {

			JpaExecutor jpaExecutor = mock(JpaExecutor.class);

			when(jpaExecutor.poll()).thenReturn("Spring");

			final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);
			assertNotNull("Expecting a Message to be returned.", jpaPollingChannelAdapter.receive());
			assertEquals("Spring", jpaPollingChannelAdapter.receive().getPayload());

	}

	/**
	*
	*/
	@Test
	public void testGetComponentType() {

			JpaExecutor jpaExecutor = mock(JpaExecutor.class);

			final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);
			assertEquals("jpa:inbound-channel-adapter", jpaPollingChannelAdapter.getComponentType());

	}

}
