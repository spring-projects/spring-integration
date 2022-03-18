/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.metadata;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.util.Assert;
import org.springframework.util.DefaultPropertiesPersister;

/**
 * Properties file-based implementation of {@link MetadataStore}. To avoid conflicts
 * each instance should be constructed with the unique key from which unique file name
 * will be generated.
 * By default, the properties file will be
 * {@code 'java.io.tmpdir' +  "/spring-integration/metadata-store.properties"},
 * but the directory and filename are settable.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class PropertiesPersistingMetadataStore implements ConcurrentMetadataStore, InitializingBean, DisposableBean,
		Closeable, Flushable {

	private static final String KEY_CANNOT_BE_NULL = "'key' cannot be null";

	private final Log logger = LogFactory.getLog(getClass());

	private final Properties metadata = new Properties();

	private final DefaultPropertiesPersister persister = new DefaultPropertiesPersister();

	private final LockRegistry lockRegistry = new DefaultLockRegistry();

	private String baseDirectory = System.getProperty("java.io.tmpdir") + "/spring-integration/";

	private String fileName = "metadata-store.properties";

	private File file;

	private volatile boolean dirty;

	/**
	 * Set the location for the properties file. Defaults to
	 * {@code 'java.io.tmpdir' +  "/spring-integration/"}.
	 * @param baseDirectory the directory.
	 */
	public void setBaseDirectory(String baseDirectory) {
		Assert.hasText(baseDirectory, "'baseDirectory' must be non-empty");
		this.baseDirectory = baseDirectory;
	}

	/**
	 * Set the name of the properties file in {@link #setBaseDirectory(String)}.
	 * Defaults to {@code metadata-store.properties},
	 * @param fileName the properties file name.
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public void afterPropertiesSet() {
		File baseDir = new File(this.baseDirectory);
		if (!baseDir.mkdirs() && !baseDir.exists() && this.logger.isWarnEnabled()) {
			this.logger.warn("Failed to create directories for " + baseDir);
		}
		this.file = new File(baseDir, this.fileName);
		try {
			if (!this.file.exists() && !this.file.createNewFile() && this.logger.isWarnEnabled()) {
				this.logger.warn("Failed to create file " + this.file);
			}
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Failed to create metadata-store file '"
					+ this.file.getAbsolutePath() + "'", ex);
		}
		loadMetadata();
	}

	@Override
	public void put(String key, String value) {
		Assert.notNull(key, KEY_CANNOT_BE_NULL);
		Assert.notNull(value, "'value' cannot be null");
		Lock lock = this.lockRegistry.obtain(key);
		lock.lock();
		try {
			this.metadata.setProperty(key, value);
		}
		finally {
			this.dirty = true;
			lock.unlock();
		}
	}

	@Override
	public String get(String key) {
		Assert.notNull(key, KEY_CANNOT_BE_NULL);
		Lock lock = this.lockRegistry.obtain(key);
		lock.lock();
		try {
			return this.metadata.getProperty(key);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public String remove(String key) {
		Assert.notNull(key, KEY_CANNOT_BE_NULL);
		Lock lock = this.lockRegistry.obtain(key);
		lock.lock();
		try {
			return (String) this.metadata.remove(key);
		}
		finally {
			this.dirty = true;
			lock.unlock();
		}
	}

	@Override
	public String putIfAbsent(String key, String value) {
		Assert.notNull(key, KEY_CANNOT_BE_NULL);
		Assert.notNull(value, "'value' cannot be null");
		Lock lock = this.lockRegistry.obtain(key);
		lock.lock();
		try {
			String property = this.metadata.getProperty(key);
			if (property == null) {
				this.metadata.setProperty(key, value);
				this.dirty = true;
				return null;
			}
			else {
				return property;
			}
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		Assert.notNull(key, KEY_CANNOT_BE_NULL);
		Assert.notNull(oldValue, "'oldValue' cannot be null");
		Assert.notNull(newValue, "'newValue' cannot be null");
		Lock lock = this.lockRegistry.obtain(key);
		lock.lock();
		try {
			String property = this.metadata.getProperty(key);
			if (oldValue.equals(property)) {
				this.metadata.setProperty(key, newValue);
				this.dirty = true;
				return true;
			}
			else {
				return false;
			}
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		flush();
	}

	@Override
	public void flush() {
		saveMetadata();
	}

	@Override
	public void destroy() {
		flush();
	}

	private void saveMetadata() {
		if (this.file == null || !this.dirty) {
			return;
		}
		this.dirty = false;
		try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(this.file))) {
			this.persister.store(this.metadata, outputStream, "Last entry");
		}
		catch (IOException ex) {
			// not fatal for the functionality of the component
			this.logger.warn("Failed to persist entry. This may result in a duplicate "
					+ "entry after this component is restarted.", ex);
		}
	}

	private void loadMetadata() {
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(this.file))) {
			this.persister.load(this.metadata, inputStream);
		}
		catch (Exception e) {
			// not fatal for the functionality of the component
			this.logger.warn("Failed to load entry from the persistent store. This may result in a duplicate " +
					"entry after this component is restarted", e);
		}
	}

}
