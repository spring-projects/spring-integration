/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.channel;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * @author Mark Fisher
 */
public class DefaultChannelRegistryTests {

	@Test
	public void testLookupRegisteredChannel() {
		QueueChannel testChannel = new QueueChannel();
		DefaultChannelRegistry registry = new DefaultChannelRegistry();
		registry.registerChannel("testChannel", testChannel);
		MessageChannel lookedUpChannel = registry.lookupChannel("testChannel");
		assertNotNull(testChannel);
		assertSame(testChannel, lookedUpChannel);
	}

	@Test
	public void testLookupNeverRegisteredChannel() {
		DefaultChannelRegistry registry = new DefaultChannelRegistry();
		MessageChannel noSuchChannel = registry.lookupChannel("noSuchChannel");
		assertNull(noSuchChannel);
	}

	@Test
	public void testLookupUnregisteredChannel() {
		QueueChannel testChannel = new QueueChannel();
		DefaultChannelRegistry registry = new DefaultChannelRegistry();
		registry.registerChannel("testChannel", testChannel);
		MessageChannel lookedUpChannel1 = registry.lookupChannel("testChannel");
		assertNotNull(lookedUpChannel1);
		assertSame(testChannel, lookedUpChannel1);
		MessageChannel unregisteredChannel = registry.unregisterChannel("testChannel");
		assertNotNull(unregisteredChannel);
		assertSame(testChannel, unregisteredChannel);
		MessageChannel lookedUpChannel2 = registry.lookupChannel("testChannel");
		assertNull(lookedUpChannel2);
	}

	@Test
	public void testUnregisteringChannelThatIsNotInRegistry() {
		DefaultChannelRegistry registry = new DefaultChannelRegistry();
		MessageChannel unregisteredChannel = registry.unregisterChannel("noSuchChannel");
		assertNull(unregisteredChannel);
	}

	@Test
	public void testUnregisteringNullDoesNotThrowException() {
		DefaultChannelRegistry registry = new DefaultChannelRegistry();
		MessageChannel unregisteredChannel = registry.unregisterChannel(null);
		assertNull(unregisteredChannel);
	}

}
