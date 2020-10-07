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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import org.springframework.context.SmartLifecycle;
import org.springframework.integration.metadata.ListenableMetadataStore;
import org.springframework.integration.metadata.MetadataStoreListener;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.util.Assert;

/**
 * Zookeeper-based {@link ListenableMetadataStore} based on a Zookeeper node.
 * Values are stored in the children node, the names of which are stored as keys.
 *
 * @author Marius Bogoevici
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class ZookeeperMetadataStore implements ListenableMetadataStore, SmartLifecycle {

	private static final String KEY_MUST_NOT_BE_NULL = "'key' must not be null.";

	private final Object lifecycleMonitor = new Object();

	private final CuratorFramework client;

	private final List<MetadataStoreListener> listeners = new CopyOnWriteArrayList<>();

	/**
	 * An internal map storing local updates, ensuring that they have precedence if the cache contains stale data.
	 * As changes are propagated back from Zookeeper to the cache, entries are removed.
	 */
	private final ConcurrentMap<String, LocalChildData> updateMap = new ConcurrentHashMap<>();

	private String root = "/SpringIntegration-MetadataStore";

	private String encoding = StandardCharsets.UTF_8.name();

	private PathChildrenCache cache;

	private boolean autoStartup = true;

	private int phase = Integer.MAX_VALUE;

	private volatile boolean running = false;

	public ZookeeperMetadataStore(CuratorFramework client) {
		Assert.notNull(client, "Client cannot be null");
		this.client = client;
	}

	/**
	 * Encoding to use when storing data in ZooKeeper
	 * @param encoding encoding as text
	 */
	public void setEncoding(String encoding) {
		Assert.hasText(encoding, "'encoding' cannot be null or empty.");
		this.encoding = encoding;
	}

	/**
	 * Root node - store entries are children of this node.
	 * @param root encoding as text
	 */
	public void setRoot(String root) {
		Assert.notNull(root, "'root' must not be null.");
		Assert.isTrue(root.charAt(0) == '/', "'root' must start with '/'");
		// remove trailing slash, if not root
		this.root = "/".equals(root) || !root.endsWith("/") ? root : root.substring(0, root.length() - 1);
	}

	public String getRoot() {
		return this.root;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public String putIfAbsent(String key, String value) {
		Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
		Assert.notNull(value, "'value' must not be null.");
		synchronized (this.updateMap) {
			try {
				createNode(key, value);
				return null;
			}
			catch (KeeperException.NodeExistsException e) {
				// so the data actually exists, we can read it
				return get(key);
			}
			catch (Exception e) {
				throw new ZookeeperMetadataStoreException("Error while trying to set '" + key + "':", e);
			}
		}
	}

	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
		Assert.notNull(oldValue, "'oldValue' must not be null.");
		Assert.notNull(newValue, "'newValue' must not be null.");
		synchronized (this.updateMap) {
			Stat currentStat = new Stat();
			try {
				byte[] bytes = this.client.getData().storingStatIn(currentStat).forPath(getPath(key));
				if (oldValue.equals(IntegrationUtils.bytesToString(bytes, this.encoding))) {
					updateNode(key, newValue, currentStat.getVersion());
				}
				return true;
			}
			catch (KeeperException.NoNodeException | KeeperException.BadVersionException e) {
				// ignore, the node doesn't exist there's nothing to replace
				return false;
			}
			// ignore
			catch (Exception e) {
				throw new ZookeeperMetadataStoreException("Cannot replace value", e);
			}
		}
	}

	@Override
	public void addListener(MetadataStoreListener listener) {
		Assert.notNull(listener, "'listener' must not be null");
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(MetadataStoreListener callback) {
		this.listeners.remove(callback);
	}

	@Override
	public void put(String key, String value) {
		Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
		Assert.notNull(value, "'value' must not be null.");
		synchronized (this.updateMap) {
			try {
				Stat currentNode = this.client.checkExists().forPath(getPath(key));
				if (currentNode == null) {
					try {
						createNode(key, value);
					}
					catch (KeeperException.NodeExistsException e) {
						updateNode(key, value, -1);
					}
				}
				else {
					updateNode(key, value, -1);
				}
			}
			catch (Exception e) {
				throw new ZookeeperMetadataStoreException("Error while setting value for key '" + key + "':", e);
			}
		}
	}

	@Override
	public String get(String key) {
		Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
		Assert.state(isRunning(), "ZookeeperMetadataStore has to be started before using.");
		synchronized (this.updateMap) {
			ChildData currentData = this.cache.getCurrentData(getPath(key));
			if (currentData == null) {
				if (this.updateMap.containsKey(key)) {
					// we have saved the value, but the cache hasn't updated yet
					// if the value had changed via replication, we would have been notified by the listener
					return this.updateMap.get(key).getValue();
				}
				else {
					// the value just doesn't exist
					return null;
				}
			}
			else {
				// our version is more recent than the cache
				if (this.updateMap.containsKey(key) &&
						this.updateMap.get(key).getVersion() >= currentData.getStat().getVersion()) {

					return this.updateMap.get(key).getValue();
				}
				return IntegrationUtils.bytesToString(currentData.getData(), this.encoding);
			}
		}
	}

	@Override
	public String remove(String key) {
		Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
		synchronized (this.updateMap) {
			try {
				byte[] bytes = this.client.getData().forPath(getPath(key));
				this.client.delete().forPath(getPath(key));
				// we guarantee that the deletion will supersede the existing data
				this.updateMap.put(key, new LocalChildData(null, Integer.MAX_VALUE));
				return IntegrationUtils.bytesToString(bytes, this.encoding);
			}
			catch (KeeperException.NoNodeException e) {
				// ignore - the node doesn't exist
				return null;
			}
			catch (Exception e) {
				throw new ZookeeperMetadataStoreException("Exception while deleting key '" + key + "'", e);
			}
		}
	}

	private void updateNode(String key, String value, int version) throws Exception { // NOSONAR external lib throws
		Stat stat = this.client.setData().withVersion(version).forPath(getPath(key),
				IntegrationUtils.stringToBytes(value, this.encoding));
		this.updateMap.put(key, new LocalChildData(value, stat.getVersion()));
	}

	private void createNode(String key, String value) throws Exception { // NOSONAR external lib throws
		this.client.create().forPath(getPath(key), IntegrationUtils.stringToBytes(value, this.encoding));
		this.updateMap.put(key, new LocalChildData(value, 0));
	}

	public String getPath(String key) {
		return "".equals(key) ? this.root : this.root + '/' + key;
	}


	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void start() {
		if (!this.running) {
			synchronized (this.lifecycleMonitor) {
				if (!this.running) {
					try {
						this.client.checkExists()
								.creatingParentContainersIfNeeded()
								.forPath(this.root);

						this.cache = new PathChildrenCache(this.client, this.root, true);
						this.cache.getListenable()
								.addListener(new MetadataStoreListenerInvokingPathChildrenCacheListener());
						this.cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
						this.running = true;
					}
					catch (Exception e) {
						throw new ZookeeperMetadataStoreException("Exception while starting bean", e);
					}
				}
			}
		}
	}

	@Override
	public void stop() {
		if (this.running) {
			synchronized (this.lifecycleMonitor) {
				if (this.running) {
					if (this.cache != null) {
						CloseableUtils.closeQuietly(this.cache);
					}
					this.cache = null;
					this.running = false;
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	private String getKey(String path) {
		return path.replace(this.root + '/', "");
	}


	private static final class LocalChildData {

		private final String value;

		private final int version;

		LocalChildData(String value, int version) {
			this.value = value;
			this.version = version;
		}

		private String getValue() {
			return this.value;
		}

		private int getVersion() {
			return this.version;
		}

	}

	private class MetadataStoreListenerInvokingPathChildrenCacheListener implements PathChildrenCacheListener {

		MetadataStoreListenerInvokingPathChildrenCacheListener() {
		}

		@Override
		public void childEvent(CuratorFramework framework, PathChildrenCacheEvent event) {
			synchronized (ZookeeperMetadataStore.this.updateMap) {
				String eventPath = event.getData().getPath();
				String eventKey = getKey(eventPath);
				String value =
						IntegrationUtils.bytesToString(event.getData().getData(), ZookeeperMetadataStore.this.encoding);
				switch (event.getType()) {
					case CHILD_ADDED:
						if (ZookeeperMetadataStore.this.updateMap.containsKey(eventKey) &&
								event.getData().getStat().getVersion() >=
										ZookeeperMetadataStore.this.updateMap.get(eventKey).getVersion()) {

							ZookeeperMetadataStore.this.updateMap.remove(eventPath);
						}
						ZookeeperMetadataStore.this.listeners.forEach((listener) -> listener.onAdd(eventKey, value));
						break;
					case CHILD_UPDATED:
						if (ZookeeperMetadataStore.this.updateMap.containsKey(eventKey) &&
								event.getData().getStat().getVersion() >=
										ZookeeperMetadataStore.this.updateMap.get(eventKey).getVersion()) {

							ZookeeperMetadataStore.this.updateMap.remove(eventPath);
						}
						ZookeeperMetadataStore.this.listeners.forEach((listener) -> listener.onUpdate(eventKey, value));
						break;
					case CHILD_REMOVED:
						ZookeeperMetadataStore.this.updateMap.remove(eventKey);
						ZookeeperMetadataStore.this.listeners.forEach((listener) -> listener.onRemove(eventKey, value));
						break;
					default:
						// ignore all other events
						break;
				}
			}
		}

	}

}
