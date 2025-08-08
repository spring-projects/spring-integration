/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.metadata;

import java.io.File;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 2.0
 */
public class PropertiesPersistingMetadataStoreTests {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

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
		File file = new File(this.folder.getRoot(), "metadata-store.properties");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.setBaseDirectory(folder.getRoot().getAbsolutePath());
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
		File file = new File(this.folder.getRoot(), "foo.properties");
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.setBaseDirectory(folder.getRoot().getAbsolutePath());
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

}
