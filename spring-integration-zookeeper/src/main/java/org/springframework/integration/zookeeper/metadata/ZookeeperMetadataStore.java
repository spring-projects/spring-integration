/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.zookeeper.metadata;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.jspecify.annotations.Nullable;

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
 * @author Christian Tzolov
 * @author Glenn Renfro
 *
 * @since 4.2
 */
public class ZookeeperMetadataStore implements ListenableMetadataStore, SmartLifecycle {

	private static final String KEY_MUST_NOT_BE_NULL = "'key' must not be null.";

	private final CuratorFramework client;

	private final List<MetadataStoreListener> listeners = new CopyOnWriteArrayList<>();

	/**
	 * An internal map storing local updates, ensuring that they have precedence if the cache contains stale data.
	 * As changes are propagated back from Zookeeper to the cache, entries are removed.
	 */
	private final ConcurrentMap<String, LocalChildData> updateMap = new ConcurrentHashMap<>();

	private String root = "/SpringIntegration-MetadataStore";

	private String encoding = StandardCharsets.UTF_8.name();

	private @Nullable CuratorCache cache;

	private boolean autoStartup = true;

	private int phase = Integer.MAX_VALUE;

	private boolean running;

	private final Lock lock = new ReentrantLock();

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
	public @Nullable String putIfAbsent(String key, String value) {
		this.lock.lock();
		try {
			Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
			Assert.notNull(value, "'value' must not be null.");
			try {
				createNode(key, value);
				return null;
			}
			catch (KeeperException.NodeExistsException ex) {
				// so the data actually exists, we can read it
				return get(key);
			}
			catch (Exception ex) {
				throw new ZookeeperMetadataStoreException("Error while trying to set '" + key + "':", ex);
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		this.lock.lock();
		try {
			Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
			Assert.notNull(oldValue, "'oldValue' must not be null.");
			Assert.notNull(newValue, "'newValue' must not be null.");
			Stat currentStat = new Stat();
			try {
				byte[] bytes = this.client.getData().storingStatIn(currentStat).forPath(getPath(key));
				if (oldValue.equals(IntegrationUtils.bytesToString(bytes, this.encoding))) {
					updateNode(key, newValue, currentStat.getVersion());
					return true;
				}
				return false;
			}
			catch (KeeperException.NoNodeException | KeeperException.BadVersionException ex) {
				// ignore, the node doesn't exist there's nothing to replace
				return false;
			}
			// ignore
			catch (Exception ex) {
				throw new ZookeeperMetadataStoreException("Cannot replace value", ex);
			}
		}
		finally {
			this.lock.unlock();
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
		this.lock.lock();
		try {
			Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
			Assert.notNull(value, "'value' must not be null.");
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
			catch (Exception ex) {
				throw new ZookeeperMetadataStoreException("Error while setting value for key '" + key + "':", ex);
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public @Nullable String get(String key) {
		this.lock.lock();
		try {
			Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
			Assert.state(isRunning(), "ZookeeperMetadataStore has to be started before using.");
			// Read the local update once and before the cache: the listener evicts it concurrently,
			// but only after the cache has been updated with data at least as recent.
			LocalChildData localData = this.updateMap.get(key);
			Optional<ChildData> cachedData = Objects.requireNonNull(this.cache).get(getPath(key));
			if (localData != null && !localData.isConfirmedBy(cachedData)) {
				// our local update is more recent than the cache
				return localData.value();
			}
			return cachedData
					.map(currentData -> IntegrationUtils.bytesToString(currentData.getData(), this.encoding)) // NOSONAR
					.orElse(null);
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public @Nullable String remove(String key) {
		this.lock.lock();
		try {
			Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
			try {
				Stat stat = new Stat();
				byte[] bytes = this.client.getData().storingStatIn(stat).forPath(getPath(key));
				this.client.delete().forPath(getPath(key));
				// the deletion supersedes the existing data until the cache confirms it
				storeLocalUpdate(key, new LocalChildData(null, stat.getCzxid(), stat.getMzxid()));
				return IntegrationUtils.bytesToString(bytes, this.encoding);
			}
			catch (KeeperException.NoNodeException ex) {
				// ignore - the node doesn't exist
				return null;
			}
			catch (Exception ex) {
				throw new ZookeeperMetadataStoreException("Exception while deleting key '" + key + "'", ex);
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	private void updateNode(String key, String value, int version) throws Exception { // NOSONAR external lib throws
		Stat stat = this.client.setData().withVersion(version).forPath(getPath(key),
				IntegrationUtils.stringToBytes(value, this.encoding));
		storeLocalUpdate(key, new LocalChildData(value, stat.getCzxid(), stat.getMzxid()));
	}

	private void createNode(String key, String value) throws Exception { // NOSONAR external lib throws
		Stat stat = new Stat();
		this.client.create().storingStatIn(stat)
				.forPath(getPath(key), IntegrationUtils.stringToBytes(value, this.encoding));
		storeLocalUpdate(key, new LocalChildData(value, stat.getCzxid(), stat.getMzxid()));
	}

	/**
	 * Store a local update and evict it right away if the cache has already caught up with it.
	 * The cache listener is called after the cache storage is updated and might process the event
	 * for this write before the entry is put into the {@link #updateMap}.
	 * Without this check such an entry would never be evicted.
	 * @param key the key for the local update.
	 * @param localData the local update data.
	 */
	private void storeLocalUpdate(String key, LocalChildData localData) {
		this.updateMap.put(key, localData);
		CuratorCache curatorCache = this.cache;
		if (curatorCache != null && localData.isConfirmedBy(curatorCache.get(getPath(key)))) {
			this.updateMap.remove(key, localData);
		}
	}

	public String getPath(String key) {
		return key.isEmpty() ? this.root : this.root + '/' + key;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void start() {
		this.lock.lock();
		try {
			if (!this.running) {
				try {
					this.client.createContainers(this.root);
					this.cache = CuratorCache.builder(this.client, this.root).build();
					this.cache.listenable().addListener(new MetadataStoreCacheListener());
					this.cache.start();
					this.running = true;
				}
				catch (Exception ex) {
					throw new ZookeeperMetadataStoreException("Exception while starting bean", ex);
				}
			}

		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void stop() {
		this.lock.lock();
		try {
			if (this.running) {
				if (this.cache != null) {
					CloseableUtils.closeQuietly(this.cache);
				}
				this.cache = null;
				this.running = false;
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public boolean isRunning() {
		this.lock.lock();
		try {
			return this.running;
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	private String getKey(String path) {
		return path.replace(this.root + '/', "");
	}

	/**
	 * The local update data.
	 * The ZooKeeper {@code czxid} identifies the node incarnation (versions restart when a node is recreated),
	 * and the {@code mzxid} orders modifications globally.
	 * @param value the stored value; {@code null} for a deletion.
	 * @param czxid the zxid of the node creation.
	 * @param mzxid the zxid of the node last modification.
	 */
	private record LocalChildData(@Nullable String value, long czxid, long mzxid) {

		/**
		 * Whether the node state (e.g. from the cache) is at least as recent as this local update.
		 * @param nodeData the current node data; empty if the node does not exist.
		 * @return true if the local update is superseded by the node data.
		 */
		boolean isConfirmedBy(Optional<ChildData> nodeData) {
			if (value() == null) {
				// the deletion is confirmed by the node absence or by its newer incarnation
				return nodeData.map(data -> data.getStat().getCzxid() > czxid()).orElse(true);
			}
			return nodeData
					.map(ChildData::getStat)
					.filter(stat ->
							stat.getCzxid() > czxid() || (stat.getCzxid() == czxid() && stat.getMzxid() >= mzxid()))
					.isPresent();
		}

		/**
		 * Whether the deletion of the node is at least as recent as this local update.
		 * @param deletedData the last known data of the deleted node.
		 * @return true if the local update is superseded by the node deletion.
		 */
		boolean isConfirmedByDeletionOf(ChildData deletedData) {
			return deletedData.getStat().getCzxid() >= czxid();
		}

	}

	private class MetadataStoreCacheListener implements CuratorCacheListener {

		MetadataStoreCacheListener() {
		}

		@Override
		public void event(Type type, ChildData oldData, ChildData newData) {
			ChildData data = Type.NODE_DELETED.equals(type) ? oldData : newData;
			String eventPath = data.getPath();
			String eventKey = getKey(eventPath);
			if (ZookeeperMetadataStore.this.root.equals(eventKey)) {
				// Ignore the root node for metadata tree
				return;
			}
			String value = IntegrationUtils.bytesToString(data.getData(), ZookeeperMetadataStore.this.encoding);

			switch (type) {
				case NODE_CREATED -> {
					evictConfirmedUpdate(eventKey, data);
					ZookeeperMetadataStore.this.listeners.forEach((listener) -> listener.onAdd(eventKey, value));
				}
				case NODE_CHANGED -> {
					evictConfirmedUpdate(eventKey, data);
					ZookeeperMetadataStore.this.listeners.forEach((listener) -> listener.onUpdate(eventKey, value));
				}
				case NODE_DELETED -> {
					ZookeeperMetadataStore.this.updateMap.computeIfPresent(eventKey,
							(k, localData) -> localData.isConfirmedByDeletionOf(data) ? null : localData);
					ZookeeperMetadataStore.this.listeners.forEach((listener) -> listener.onRemove(eventKey, value));
				}
			}
		}

		private void evictConfirmedUpdate(String key, ChildData eventData) {
			ZookeeperMetadataStore.this.updateMap.computeIfPresent(key,
					(k, localData) -> localData.isConfirmedBy(Optional.of(eventData)) ? null : localData);
		}

	}

}
