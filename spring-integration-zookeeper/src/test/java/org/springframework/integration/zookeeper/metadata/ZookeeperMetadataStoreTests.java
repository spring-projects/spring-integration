/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.zookeeper.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.CloseableUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
	@BeforeEach
	public void setUp() {
		super.setUp();
		this.metadataStore = new ZookeeperMetadataStore(client);
		this.metadataStore.start();
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		this.metadataStore.stop();
		this.client.delete().deletingChildrenIfNeeded().forPath(this.metadataStore.getRoot());
	}

	@Test
	public void testGetNonExistingKeyValue() {
		String retrievedValue = metadataStore.get("does-not-exist");
		assertThat(retrievedValue).isNull();
	}

	@Test
	public void testPersistKeyValue() throws Exception {
		String testKey = "ZookeeperMetadataStoreTests-Persist";
		metadataStore.put(testKey, "Integration");
		assertThat(client.checkExists().forPath(metadataStore.getPath(testKey))).isNotNull();
		assertThat(IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey)), "UTF-8"))
				.isEqualTo("Integration");
	}


	@Test
	public void testGetValueFromMetadataStore() {
		String testKey = "ZookeeperMetadataStoreTests-GetValue";
		metadataStore.put(testKey, "Hello Zookeeper");
		String retrievedValue = metadataStore.get(testKey);
		assertThat(retrievedValue).isEqualTo("Hello Zookeeper");
	}


	@Test
	public void testPutIfAbsent() throws Exception {
		final String testKey = "ZookeeperMetadataStoreTests-Persist";
		final String testKey2 = "ZookeeperMetadataStoreTests-Persist-2";
		metadataStore.put(testKey, "Integration");
		assertThat(client.checkExists().forPath(metadataStore.getPath(testKey))).isNotNull();
		assertThat(IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey)), "UTF-8"))
				.isEqualTo("Integration");
		CuratorFramework otherClient = createNewClient();
		final ZookeeperMetadataStore otherMetadataStore = new ZookeeperMetadataStore(otherClient);
		otherMetadataStore.start();
		otherMetadataStore.putIfAbsent(testKey, "OtherValue");
		assertThat(IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey)), "UTF-8"))
				.isEqualTo("Integration");
		assertThat(metadataStore.get(testKey)).isEqualTo("Integration");
		await().untilAsserted(() -> assertThat("Integration").isEqualTo(otherMetadataStore.get(testKey)));
		otherMetadataStore.putIfAbsent(testKey2, "Integration-2");
		assertThat(IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey2)), "UTF-8"))
				.isEqualTo("Integration-2");
		assertThat(otherMetadataStore.get(testKey2)).isEqualTo("Integration-2");
		await().untilAsserted(() -> assertThat("Integration-2").isEqualTo(otherMetadataStore.get(testKey2)));

		otherMetadataStore.stop();
		CloseableUtils.closeQuietly(otherClient);

	}

	@Test
	public void testReplace() throws Exception {
		final String testKey = "ZookeeperMetadataStoreTests-Replace";
		metadataStore.put(testKey, "Integration");
		assertThat(client.checkExists().forPath(metadataStore.getPath(testKey))).isNotNull();
		assertThat(IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey)), "UTF-8"))
				.isEqualTo("Integration");
		CuratorFramework otherClient = createNewClient();
		final ZookeeperMetadataStore otherMetadataStore = new ZookeeperMetadataStore(otherClient);
		otherMetadataStore.start();
		otherMetadataStore.replace(testKey, "OtherValue", "Integration-2");
		assertThat(IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey)), "UTF-8"))
				.isEqualTo("Integration");
		assertThat(metadataStore.get(testKey)).isEqualTo("Integration");
		await().untilAsserted(() -> assertThat("Integration").isEqualTo(otherMetadataStore.get(testKey)));
		otherMetadataStore.replace(testKey, "Integration", "Integration-2");
		assertThat(IntegrationUtils.bytesToString(client.getData().forPath(metadataStore.getPath(testKey)), "UTF-8"))
				.isEqualTo("Integration-2");
		await().untilAsserted(() -> assertThat("Integration-2").isEqualTo(otherMetadataStore.get(testKey)));
		assertThat(otherMetadataStore.get(testKey)).isEqualTo("Integration-2");

		otherMetadataStore.stop();
		CloseableUtils.closeQuietly(otherClient);
	}

	@Test
	public void testPersistEmptyStringToMetadataStore() {
		String testKey = "ZookeeperMetadataStoreTests-PersistEmpty";
		metadataStore.put(testKey, "");
		assertThat(metadataStore.get(testKey)).isEqualTo("");
	}

	@Test
	public void testPersistNullStringToMetadataStore() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> metadataStore.put("ZookeeperMetadataStoreTests-PersistEmpty", null))
				.withMessage("'value' must not be null.");
	}

	@Test
	public void testPersistWithEmptyKeyToMetadataStore() {
		metadataStore.put("", "PersistWithEmptyKey");
		String retrievedValue = metadataStore.get("");
		assertThat(retrievedValue).isEqualTo("PersistWithEmptyKey");
	}

	@Test
	public void testPersistWithNullKeyToMetadataStore() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> metadataStore.put(null, "something"))
				.withMessage("'key' must not be null.");
	}

	@Test
	public void testGetValueWithNullKeyFromMetadataStore() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> metadataStore.get(null))
				.withMessage("'key' must not be null.");
	}

	@Test
	public void testRemoveFromMetadataStore() {
		String testKey = "ZookeeperMetadataStoreTests-Remove";
		String testValue = "Integration";
		metadataStore.put(testKey, testValue);
		assertThat(metadataStore.remove(testKey)).isEqualTo(testValue);
		assertThat(metadataStore.remove(testKey)).isNull();
	}

	@Test
	public void testListenerInvokedOnLocalChanges() {
		String testKey = "ZookeeperMetadataStoreTests";

		// register listeners
		final List<List<String>> notifiedChanges = new ArrayList<>();
		final Map<String, CyclicBarrier> barriers = new HashMap<>();
		barriers.put("add", new CyclicBarrier(2));
		barriers.put("remove", new CyclicBarrier(2));
		barriers.put("update", new CyclicBarrier(2));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> metadataStore.addListener(null))
				.withMessageContaining("'listener' must not be null");

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
		assertThat(notifiedChanges).hasSize(1);
		assertThat(notifiedChanges.get(0)).containsExactly("add", testKey, "Integration");

		metadataStore.putIfAbsent(testKey, "Integration++");
		// there is no update and therefore we expect no changes
		assertThat(notifiedChanges).hasSize(1);

		barriers.get("update").reset();
		metadataStore.put(testKey, "Integration-2");
		waitAtBarrier("update", barriers);
		assertThat(notifiedChanges).hasSize(2);
		assertThat(notifiedChanges.get(1)).containsExactly("update", testKey, "Integration-2");

		barriers.get("update").reset();
		metadataStore.replace(testKey, "Integration-2", "Integration-3");
		waitAtBarrier("update", barriers);
		assertThat(notifiedChanges).hasSize(3);
		assertThat(notifiedChanges.get(2)).containsExactly("update", testKey, "Integration-3");

		metadataStore.replace(testKey, "Integration-2", "Integration-none");
		assertThat(notifiedChanges).hasSize(3);

		barriers.get("remove").reset();
		metadataStore.remove(testKey);
		waitAtBarrier("remove", barriers);
		assertThat(notifiedChanges).hasSize(4);
		assertThat(notifiedChanges.get(3)).containsExactly("remove", testKey, "Integration-3");
	}

	@Test
	public void testListenerInvokedOnRemoteChanges() {
		String testKey = "ZookeeperMetadataStoreTests";

		CuratorFramework otherClient = createNewClient();
		ZookeeperMetadataStore otherMetadataStore = new ZookeeperMetadataStore(otherClient);
		otherMetadataStore.start();

		// register listeners
		final List<List<String>> notifiedChanges = new ArrayList<>();
		final Map<String, CyclicBarrier> barriers = new HashMap<>();
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
		assertThat(notifiedChanges).hasSize(1);
		assertThat(notifiedChanges.get(0)).containsExactly("add", testKey, "Integration");

		otherMetadataStore.putIfAbsent(testKey, "Integration++");
		// there is no update and therefore we expect no changes
		assertThat(notifiedChanges).hasSize(1);

		barriers.get("update").reset();
		otherMetadataStore.put(testKey, "Integration-2");
		waitAtBarrier("update", barriers);
		assertThat(notifiedChanges).hasSize(2);
		assertThat(notifiedChanges.get(1)).containsExactly("update", testKey, "Integration-2");

		barriers.get("update").reset();
		otherMetadataStore.replace(testKey, "Integration-2", "Integration-3");
		waitAtBarrier("update", barriers);
		assertThat(notifiedChanges).hasSize(3);
		assertThat(notifiedChanges.get(2)).containsExactly("update", testKey, "Integration-3");

		otherMetadataStore.replace(testKey, "Integration-2", "Integration-none");
		assertThat(notifiedChanges).hasSize(3);

		barriers.get("remove").reset();
		otherMetadataStore.remove(testKey);
		waitAtBarrier("remove", barriers);
		assertThat(notifiedChanges).hasSize(4);
		assertThat(notifiedChanges.get(3)).containsExactly("remove", testKey, "Integration-3");

		otherMetadataStore.stop();
		CloseableUtils.closeQuietly(otherClient);

	}

	@Test
	public void testAddRemoveListener() {
		MetadataStoreListener mockListener = Mockito.mock(MetadataStoreListener.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(metadataStore);

		@SuppressWarnings("unchecked")
		List<MetadataStoreListener> listeners = (List<MetadataStoreListener>) accessor.getPropertyValue("listeners");

		assertThat(listeners).hasSize(0);
		metadataStore.addListener(mockListener);
		assertThat(listeners).hasSize(1);
		assertThat(listeners).containsExactly(mockListener);
		metadataStore.removeListener(mockListener);
		assertThat(listeners).hasSize(0);
	}

	@Test
	public void testEnsureStarted() {
		ZookeeperMetadataStore zookeeperMetadataStore = new ZookeeperMetadataStore(this.client);
		assertThatIllegalStateException()
				.isThrownBy(() -> zookeeperMetadataStore.get("foo"))
				.withMessageContaining("ZookeeperMetadataStore has to be started before using.");
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
