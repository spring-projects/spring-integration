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

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.Executors;

import org.junit.Test;

/**
 * @author Mark Fisher
 */
public class RetrievalBlockingMessageStoreTests {

	@Test
	public void testGetWithElapsedTimeout() {
		final RetrievalBlockingMessageStore store = new RetrievalBlockingMessageStore(10);
		publishWithDelay(store, "foo", "bar", 500);
		Message<?> message = store.get("foo", 5);
		assertNull(message);
	}

	@Test
	public void testWrappedTargetGetWithElapsedTimeout() {
		MessageStore target = new SimpleMessageStore(10);
		final RetrievalBlockingMessageStore store = new RetrievalBlockingMessageStore(target);
		publishWithDelay(store, "foo", "bar", 500);
		Message<?> message = store.get("foo", 5);
		assertNull(message);
	}

	@Test
	public void testGetWithinTimeout() {
		final RetrievalBlockingMessageStore store = new RetrievalBlockingMessageStore(10);
		publishWithDelay(store, "foo", "bar", 50);
		Message<?> message = store.get("foo", 500);
		assertNotNull(message);
		assertEquals("bar", message.getPayload());
		assertNotNull(store.get("foo", 0));
	}

	@Test
	public void testWrappedTargetGetWithinTimeout() {
		MessageStore target = new SimpleMessageStore(10);
		final RetrievalBlockingMessageStore store = new RetrievalBlockingMessageStore(target);
		publishWithDelay(store, "foo", "bar", 50);
		Message<?> message = store.get("foo", 500);
		assertNotNull(message);
		assertEquals("bar", message.getPayload());
		assertNotNull(store.get("foo", 0));
	}

	@Test
	public void testRemoveWithElapsedTimeout() {
		final RetrievalBlockingMessageStore store = new RetrievalBlockingMessageStore(10);
		publishWithDelay(store, "foo", "bar", 500);
		Message<?> message = store.remove("foo", 5);
		assertNull(message);
	}

	@Test
	public void testWrappedMessageStoreRemoveWithElapsedTimeout() {
		MessageStore target = new SimpleMessageStore(10);
		final RetrievalBlockingMessageStore store = new RetrievalBlockingMessageStore(target);
		publishWithDelay(store, "foo", "bar", 500);
		Message<?> message = store.remove("foo", 5);
		assertNull(message);
	}

	@Test
	public void testRemoveWithinTimeout() {
		final RetrievalBlockingMessageStore store = new RetrievalBlockingMessageStore(10);
		publishWithDelay(store, "foo", "bar", 50);
		Message<?> message = store.remove("foo", 1000);
		assertNotNull(message);
		assertEquals("bar", message.getPayload());
		assertNull(store.get("foo", 0));
	}

	@Test
	public void testWrappedMessageStoreRemoveWithinTimeout() {
		MessageStore target = new SimpleMessageStore(10);
		final RetrievalBlockingMessageStore store = new RetrievalBlockingMessageStore(target);
		publishWithDelay(store, "foo", "bar", 50);
		Message<?> message = store.remove("foo", 1000);
		assertNotNull(message);
		assertEquals("bar", message.getPayload());
		assertNull(store.get("foo", 0));
	}


	private static void publishWithDelay(final MessageStore store, final String key, final String value, final long delay) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					Thread.sleep(delay);
				}
				catch (InterruptedException e) {}
				store.put(key, new StringMessage(value));
			}
		});
	}

}
