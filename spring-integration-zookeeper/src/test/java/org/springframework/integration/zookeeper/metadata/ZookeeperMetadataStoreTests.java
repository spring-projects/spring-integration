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

package org.springframework.integration.zookeeper.metadata;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.integration.test.matcher.EqualsResultMatcher.equalsResult;
import static org.springframework.integration.test.matcher.EventuallyMatcher.eventually;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.CloseableUtils;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.metadata.MetadataStoreListener;
import org.springframework.integration.metadata.MetadataStoreListenerAdapter;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.zookeeper.ZookeeperTestSupport;

/**
 * @author Marius Bogoevici
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class ZookeeperMetadataStoreTests extends ZookeeperTestSupport {

	private ZookeeperMetadataStore metadataStore;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		this.metadataStore = new ZookeeperMetadataStore(client);
		this.metadataStore.start();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		this.metadataStore.stop();
		this.client.delete().deletingChildrenIfNeeded().forPath(this.metadataStore.getRoot());
	}

	@Test
	public void testGetNonExistingKeyValue() {
		String retrievedValue = metadataStore.get("does-not-exist");
		assertNull(retrievedValue);
	}

	@Test
	public void testPersistKeyValue() throws Exception {
		String testKey = "ZookeeperMetadataStoreTests-Persist";
		metadataStore.put(testKey, "Integration");
		assertNotNull(client.checkExists().forPath(metadataStore.getPath(testKey)));
		assertEquals("Integration",
				IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey)), "UTF-8"));
	}


	@Test
	public void testGetValueFromMetadataStore() throws Exception {
		String testKey = "ZookeeperMetadataStoreTests-GetValue";
		metadataStore.put(testKey, "Hello Zookeeper");
		String retrievedValue = metadataStore.get(testKey);
		assertEquals("Hello Zookeeper", retrievedValue);
	}


	@Test
	public void testPutIfAbsent() throws Exception {
		final String testKey = "ZookeeperMetadataStoreTests-Persist";
		final String testKey2 = "ZookeeperMetadataStoreTests-Persist-2";
		metadataStore.put(testKey, "Integration");
		assertNotNull(client.checkExists().forPath(metadataStore.getPath(testKey)));
		assertEquals("Integration",
				IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey)), "UTF-8"));
		CuratorFramework otherClient = createNewClient();
		final ZookeeperMetadataStore otherMetadataStore = new ZookeeperMetadataStore(otherClient);
		otherMetadataStore.start();
		otherMetadataStore.putIfAbsent(testKey, "OtherValue");
		assertEquals("Integration",
				IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey)), "UTF-8"));
		assertEquals("Integration", metadataStore.get(testKey));
		assertThat("Integration", eventually(equalsResult(() -> otherMetadataStore.get(testKey))));
		otherMetadataStore.putIfAbsent(testKey2, "Integration-2");
		assertEquals("Integration-2",
				IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey2)), "UTF-8"));
		assertEquals("Integration-2", otherMetadataStore.get(testKey2));
		assertThat("Integration-2", eventually(equalsResult(() -> otherMetadataStore.get(testKey2))));

		otherMetadataStore.stop();
		CloseableUtils.closeQuietly(otherClient);

	}

	@Test
	public void testReplace() throws Exception {
		final String testKey = "ZookeeperMetadataStoreTests-Replace";
		metadataStore.put(testKey, "Integration");
		assertNotNull(client.checkExists().forPath(metadataStore.getPath(testKey)));
		assertEquals("Integration",
				IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey)), "UTF-8"));
		CuratorFramework otherClient = createNewClient();
		final ZookeeperMetadataStore otherMetadataStore = new ZookeeperMetadataStore(otherClient);
		otherMetadataStore.start();
		otherMetadataStore.replace(testKey, "OtherValue", "Integration-2");
		assertEquals("Integration",
				IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey)), "UTF-8"));
		assertEquals("Integration", metadataStore.get(testKey));
		assertThat("Integration", eventually(equalsResult(() -> otherMetadataStore.get(testKey))));
		otherMetadataStore.replace(testKey, "Integration", "Integration-2");
		assertEquals("Integration-2",
				IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey)), "UTF-8"));
		assertThat("Integration-2", eventually(equalsResult(() -> metadataStore.get(testKey))));
		assertEquals("Integration-2", otherMetadataStore.get(testKey));

		otherMetadataStore.stop();
		CloseableUtils.closeQuietly(otherClient);
	}

	@Test
	public void testPersistEmptyStringToMetadataStore() {
		String testKey = "ZookeeperMetadataStoreTests-PersistEmpty";
		metadataStore.put(testKey, "");
		assertEquals("", metadataStore.get(testKey));
	}

	@Test
	public void testPersistNullStringToMetadataStore() {
		try {
			metadataStore.put("ZookeeperMetadataStoreTests-PersistEmpty", null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("'value' must not be null.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testPersistWithEmptyKeyToMetadataStore() {
		metadataStore.put("", "PersistWithEmptyKey");
		String retrievedValue = metadataStore.get("");
		assertEquals("PersistWithEmptyKey", retrievedValue);
	}

	@Test
	public void testPersistWithNullKeyToMetadataStore() {
		try {
			metadataStore.put(null, "something");
		}
		catch (IllegalArgumentException e) {
			assertEquals("'key' must not be null.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testGetValueWithNullKeyFromMetadataStore() {
		try {
			metadataStore.get(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("'key' must not be null.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testRemoveFromMetadataStore() throws Exception {
		String testKey = "ZookeeperMetadataStoreTests-Remove";
		String testValue = "Integration";
		metadataStore.put(testKey, testValue);
		assertEquals(testValue, metadataStore.remove(testKey));
		assertNull(metadataStore.remove(testKey));
	}

	@Test
	public void testListenerInvokedOnLocalChanges() throws Exception {
		String testKey = "ZookeeperMetadataStoreTests";

		// register listeners
		final List<List<String>> notifiedChanges = new ArrayList<List<String>>();
		final Map<String, CyclicBarrier> barriers = new HashMap<String, CyclicBarrier>();
		barriers.put("add", new CyclicBarrier(2));
		barriers.put("remove", new CyclicBarrier(2));
		barriers.put("update", new CyclicBarrier(2));
		try {
			metadataStore.addListener(null);
			fail("IllegalArgumentException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalArgumentException.class));
			assertThat(e.getMessage(), containsString("'listener' must not be null"));
		}
		metadataStore.addListener(new MetadataStoreListenerAdapter() {

			@Override
			public void onAdd(String key, String value) {
				notifiedChanges.add(Arrays.asList("add", key, value));
				waitAtBarrier("add", barriers);
			}

			@Override
			public void onRemove(String key, String oldValue) {
				notifiedChanges.add(Arrays.asList("remove", key, oldValue));
				waitAtBarrier("remove", barriers);
			}

			@Override
			public void onUpdate(String key, String newValue) {
				notifiedChanges.add(Arrays.asList("update", key, newValue));
				waitAtBarrier("update", barriers);
			}
		});

		// the tests themselves
		barriers.get("add").reset();
		metadataStore.put(testKey, "Integration");
		waitAtBarrier("add", barriers);
		assertThat(notifiedChanges, hasSize(1));
		assertThat(notifiedChanges.get(0), IsIterableContainingInOrder.contains("add", testKey, "Integration"));

		metadataStore.putIfAbsent(testKey, "Integration++");
		// there is no update and therefore we expect no changes
		assertThat(notifiedChanges, hasSize(1));

		barriers.get("update").reset();
		metadataStore.put(testKey, "Integration-2");
		waitAtBarrier("update", barriers);
		assertThat(notifiedChanges, hasSize(2));
		assertThat(notifiedChanges.get(1), IsIterableContainingInOrder.contains("update", testKey, "Integration-2"));

		barriers.get("update").reset();
		metadataStore.replace(testKey, "Integration-2", "Integration-3");
		waitAtBarrier("update", barriers);
		assertThat(notifiedChanges, hasSize(3));
		assertThat(notifiedChanges.get(2), IsIterableContainingInOrder.contains("update", testKey, "Integration-3"));

		metadataStore.replace(testKey, "Integration-2", "Integration-none");
		assertThat(notifiedChanges, hasSize(3));

		barriers.get("remove").reset();
		metadataStore.remove(testKey);
		waitAtBarrier("remove", barriers);
		assertThat(notifiedChanges, hasSize(4));
		assertThat(notifiedChanges.get(3), IsIterableContainingInOrder.contains("remove", testKey, "Integration-3"));
	}

	@Test
	public void testListenerInvokedOnRemoteChanges() throws Exception {
		String testKey = "ZookeeperMetadataStoreTests";

		CuratorFramework otherClient = createNewClient();
		ZookeeperMetadataStore otherMetadataStore = new ZookeeperMetadataStore(otherClient);

		// register listeners
		final List<List<String>> notifiedChanges = new ArrayList<List<String>>();
		final Map<String, CyclicBarrier> barriers = new HashMap<String, CyclicBarrier>();
		barriers.put("add", new CyclicBarrier(2));
		barriers.put("remove", new CyclicBarrier(2));
		barriers.put("update", new CyclicBarrier(2));
		metadataStore.addListener(new MetadataStoreListenerAdapter() {

			@Override
			public void onAdd(String key, String value) {
				notifiedChanges.add(Arrays.asList("add", key, value));
				waitAtBarrier("add", barriers);
			}

			@Override
			public void onRemove(String key, String oldValue) {
				notifiedChanges.add(Arrays.asList("remove", key, oldValue));
				waitAtBarrier("remove", barriers);
			}

			@Override
			public void onUpdate(String key, String newValue) {
				notifiedChanges.add(Arrays.asList("update", key, newValue));
				waitAtBarrier("update", barriers);
			}
		});

		// the tests themselves
		barriers.get("add").reset();
		otherMetadataStore.put(testKey, "Integration");
		waitAtBarrier("add", barriers);
		assertThat(notifiedChanges, hasSize(1));
		assertThat(notifiedChanges.get(0), IsIterableContainingInOrder.contains("add", testKey, "Integration"));

		otherMetadataStore.putIfAbsent(testKey, "Integration++");
		// there is no update and therefore we expect no changes
		assertThat(notifiedChanges, hasSize(1));

		barriers.get("update").reset();
		otherMetadataStore.put(testKey, "Integration-2");
		waitAtBarrier("update", barriers);
		assertThat(notifiedChanges, hasSize(2));
		assertThat(notifiedChanges.get(1), IsIterableContainingInOrder.contains("update", testKey, "Integration-2"));

		barriers.get("update").reset();
		otherMetadataStore.replace(testKey, "Integration-2", "Integration-3");
		waitAtBarrier("update", barriers);
		assertThat(notifiedChanges, hasSize(3));
		assertThat(notifiedChanges.get(2), IsIterableContainingInOrder.contains("update", testKey, "Integration-3"));

		otherMetadataStore.replace(testKey, "Integration-2", "Integration-none");
		assertThat(notifiedChanges, hasSize(3));

		barriers.get("remove").reset();
		otherMetadataStore.remove(testKey);
		waitAtBarrier("remove", barriers);
		assertThat(notifiedChanges, hasSize(4));
		assertThat(notifiedChanges.get(3), IsIterableContainingInOrder.contains("remove", testKey, "Integration-3"));

		otherMetadataStore.stop();
		CloseableUtils.closeQuietly(otherClient);

	}

	@Test
	public void testAddRemoveListener() throws Exception {
		MetadataStoreListener mockListener = Mockito.mock(MetadataStoreListener.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(metadataStore);

		@SuppressWarnings("unchecked")
		List<MetadataStoreListener> listeners = (List<MetadataStoreListener>) accessor.getPropertyValue("listeners");

		assertThat(listeners, hasSize(0));
		metadataStore.addListener(mockListener);
		assertThat(listeners, hasSize(1));
		assertThat(listeners, IsIterableContainingInOrder.contains(mockListener));
		metadataStore.removeListener(mockListener);
		assertThat(listeners, hasSize(0));
	}

	@Test
	public void testEnsureStarted() {
		ZookeeperMetadataStore zookeeperMetadataStore = new ZookeeperMetadataStore(this.client);

		try {
			zookeeperMetadataStore.get("foo");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalStateException.class));
			assertThat(e.getMessage(), containsString("ZookeeperMetadataStore has to be started before using."));
		}
	}

	private void waitAtBarrier(String barrierName, Map<String, CyclicBarrier> barriers) {
		try {
			barriers.get(barrierName).await(10, TimeUnit.SECONDS);
		}
		catch (Exception e) {
			throw new AssertionError("Test didn't complete: ", e);
		}
	}

}
