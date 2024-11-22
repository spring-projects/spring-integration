/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.File;
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

}
