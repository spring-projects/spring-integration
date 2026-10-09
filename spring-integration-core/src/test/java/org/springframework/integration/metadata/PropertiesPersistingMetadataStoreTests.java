/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.metadata;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Uwez Khan
 * @author Glenn Renfro
 *
 * @since 2.0
 */
public class PropertiesPersistingMetadataStoreTests {

	@TempDir
	static File folder;

	@Test
	public void validateWithDefaultBaseDir() throws Exception {
		File file = new File(System.getProperty("java.io.tmpdir") + "/spring-integration/metadata-store.properties");
		file.delete();
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		assertThat(file.exists()).isTrue();
		assertThat(metadataStore.putIfAbsent("foo", "baz")).isNull();
		assertThat(metadataStore.putIfAbsent("foo", "baz")).isNotNull();
		assertThat(metadataStore.replace("foo", "xxx", "bar")).isFalse();
		assertThat(metadataStore.replace("foo", "baz", "bar")).isTrue();
		metadataStore.close();
		Properties persistentProperties = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
		assertThat(persistentProperties).isNotNull();
		assertThat(persistentProperties.size()).isEqualTo(1);
		assertThat(persistentProperties.get("foo")).isEqualTo("bar");
		file.delete();
	}

	@Test
	public void validateWithCustomBaseDir() throws Exception {
		File file = new File(folder, "metadata-store.properties");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.setBaseDirectory(folder.getAbsolutePath());
		metadataStore.afterPropertiesSet();
		metadataStore.put("foo", "bar");
		metadataStore.close();
		assertThat(file.exists()).isTrue();
		Properties persistentProperties = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
		assertThat(persistentProperties).isNotNull();
		assertThat(persistentProperties.size()).isEqualTo(1);
		assertThat(persistentProperties.get("foo")).isEqualTo("bar");
	}

	@Test
	public void validateWithCustomFileName() throws Exception {
		File file = new File(folder, "foo.properties");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.setBaseDirectory(folder.getAbsolutePath());
		metadataStore.setFileName("foo.properties");
		metadataStore.afterPropertiesSet();
		metadataStore.put("foo", "bar");
		metadataStore.close();
		assertThat(file.exists()).isTrue();
		Properties persistentProperties = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
		assertThat(persistentProperties).isNotNull();
		assertThat(persistentProperties.size()).isEqualTo(1);
		assertThat(persistentProperties.get("foo")).isEqualTo("bar");
	}

	@Test
	public void flushRetriesAfterFailedPersisting() throws Exception {
		File baseDir = new File(folder, "metadata-retry");
		File file = new File(baseDir, "metadata-store.properties");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.setBaseDirectory(baseDir.getAbsolutePath());
		metadataStore.afterPropertiesSet();
		metadataStore.put("lastProcessedId", "42");

		Files.delete(file.toPath());
		Files.delete(baseDir.toPath());
		metadataStore.flush();

		Files.createDirectory(baseDir.toPath());
		metadataStore.flush();

		PropertiesPersistingMetadataStore restored = new PropertiesPersistingMetadataStore();
		restored.setBaseDirectory(baseDir.getAbsolutePath());
		restored.afterPropertiesSet();

		assertThat(restored.get("lastProcessedId")).isEqualTo("42");
	}

	@Test
	public void metadataStoreFileAndCreatedDirectoryAreOwnerOnly() throws Exception {
		File baseDir = new File(folder, "secure-store");
		File file = new File(baseDir, "metadata-store.properties");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.setBaseDirectory(baseDir.getAbsolutePath());
		metadataStore.afterPropertiesSet();
		metadataStore.put("lastProcessedId", "42");
		metadataStore.close();

		assertThat(file).exists();

		assertThat(file).canRead().canWrite();
		assertThat(baseDir).canRead().canWrite().isExecutable();
	}

}
