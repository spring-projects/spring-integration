/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.context.metadata;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.util.Properties;

import org.junit.Test;

/**
 * @author Oleg Zhurakousky
 *
 */
public class FileBasedPropertiesStoreTests {

	@Test(expected=IllegalArgumentException.class)
	public void validateFailureWithNoPersistentKey(){
		new FileBasedPropertiesStore(null);
	}
	
	@Test
	public void validateWithDefaultBaseDir() throws Exception{
		File file = new File(System.getProperty("java.io.tmpdir") + "/spring-integration/" + "foo.last.entry");
		file.delete();
		FileBasedPropertiesStore metaStore = new FileBasedPropertiesStore("foo");
		metaStore.afterPropertiesSet();
		assertTrue(file.exists());
		Properties prop = new Properties();
		prop.setProperty("foo", "bar");
		metaStore.write(prop);
		Properties persistentProperties = metaStore.load();
		assertNotNull(persistentProperties);
		assertEquals(1, persistentProperties.size());
		assertEquals("bar", persistentProperties.get("foo"));
	}
	@Test
	public void validateWithCustomBaseDir() throws Exception{
		File file = new File("foo/" + "foo.last.entry");
		file.delete();
		file.deleteOnExit();
		FileBasedPropertiesStore metaStore = new FileBasedPropertiesStore("foo");
		metaStore.setBaseDirectory("foo"); 
		metaStore.afterPropertiesSet();
		assertTrue(file.exists());
		Properties prop = new Properties();
		prop.setProperty("foo", "bar");
		metaStore.write(prop);
		Properties persistentProperties = metaStore.load();
		assertNotNull(persistentProperties);
		assertEquals(1, persistentProperties.size());
		assertEquals("bar", persistentProperties.get("foo"));
	}
}
