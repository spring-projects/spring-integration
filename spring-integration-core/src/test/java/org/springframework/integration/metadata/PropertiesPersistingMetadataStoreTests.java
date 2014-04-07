/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Properties;

import org.junit.Test;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 2.0
 */
public class PropertiesPersistingMetadataStoreTests {

	@Test
	public void validateWithDefaultBaseDir() throws Exception {
		File file = new File(System.getProperty("java.io.tmpdir") + "/spring-integration/metadata-store.properties");
		file.delete();
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		assertTrue(file.exists());
		assertNull(metadataStore.putIfAbsent("foo", "baz"));
		assertNotNull(metadataStore.putIfAbsent("foo", "baz"));
		assertFalse(metadataStore.replace("foo", "xxx", "bar"));
		assertTrue(metadataStore.replace("foo", "baz", "bar"));
		metadataStore.destroy();
		Properties persistentProperties = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
		assertNotNull(persistentProperties);
		assertEquals(1, persistentProperties.size());
		assertEquals("bar", persistentProperties.get("foo"));
		file.delete();
	}

	@Test
	public void validateWithCustomBaseDir() throws Exception {
		File file = new File("target/foo" + "/metadata-store.properties");
		file.deleteOnExit();
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.setBaseDirectory("target/foo");
		metadataStore.afterPropertiesSet();
		metadataStore.put("foo", "bar");
		metadataStore.destroy();
		assertTrue(file.exists());
		Properties persistentProperties = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
		assertNotNull(persistentProperties);
		assertEquals(1, persistentProperties.size());
		assertEquals("bar", persistentProperties.get("foo"));
	}

}
