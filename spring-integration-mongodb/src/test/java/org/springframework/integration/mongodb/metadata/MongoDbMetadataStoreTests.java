/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.integration.mongodb.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;


/**
 * @author SenthilArumugam SamirajPanneerSelvam
 *
 */
public class MongoDbMetadataStoreTests extends MongoDbAvailableTests {
	
	private MongoDbMetadataStore store = null;
	private MongoDbFactory mongoDbFactory = null;
	private final static String DEFAULT_COLLECTION_NAME = "metadatastore";
	private String file1 = "/remotepath/filesTodownload/file-1.txt";
	private String file1Id = "12345";

	@MongoDbAvailable
	@Before
	public void configure() throws Exception{
		mongoDbFactory = this.prepareMongoFactory(DEFAULT_COLLECTION_NAME);
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		store = new MongoDbMetadataStore(template);
	}
	
	@Test
	@MongoDbAvailable
	public void testGetFromStore(){
		String fileID = store.get(file1);
		assertNull(fileID);
		
		store.put(file1, file1Id);
		fileID = store.get(file1);
		assertNotNull(fileID);
		assertEquals(file1Id, fileID);
	}
	
	@Test
	@MongoDbAvailable
	public void testPutIfAbsent(){
		String fileID = store.get(file1);
		assertNull(fileID);
		
		fileID = store.putIfAbsent(file1, file1Id);
		assertNotNull(fileID);
		assertEquals(file1Id, fileID);
	}
	
	@Test
	@MongoDbAvailable
	public void testRemove(){
		String fileID = store.remove(file1);
		assertNull(fileID);
		
		fileID = store.putIfAbsent(file1, file1Id);
		assertNotNull(fileID);
		assertEquals(file1Id, fileID);
		
		fileID = store.remove(file1);
		assertNotNull(fileID);
		assertEquals(file1Id, fileID);
	}
	
	@Test
	@MongoDbAvailable
	public void testReplace(){
		boolean removedValue = store.replace(file1, file1Id, "4567");
		assertFalse(removedValue);
		
		String fileID = store.putIfAbsent(file1, file1Id);
		assertNotNull(fileID);
		assertEquals(file1Id, fileID);
		
		removedValue = store.replace(file1, file1Id, "4567");
		assertTrue(removedValue);
	}
	
}
