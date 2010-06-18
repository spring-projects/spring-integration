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
package org.springframework.integration.core;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Oleg Zhurakousky
 *
 */
public class TimeBasedUUIDGeneratorTests {

	@Test
	public void testGreaterThen(){
		UUID id = TimeBasedUUIDGenerator.generateId();
		for (int i = 0; i < 1000; i++) {
			UUID newId = TimeBasedUUIDGenerator.generateId();
			// tests, that newly created UUID is always greater then the previous one.
			Assert.assertTrue(newId.compareTo(id) == 1);
			id = newId;
		}
	}
	@Test
	public void testUniqueness(){
		long timestamp = System.currentTimeMillis();
		UUID id = TimeBasedUUIDGenerator.generateIdFromTimestamp(timestamp);
		for (int i = 0; i < 1000; i++) {
			UUID newId = TimeBasedUUIDGenerator.generateIdFromTimestamp(timestamp);
			// tests, that newly created UUID is always greater then the previous one.
			Assert.assertTrue(newId.compareTo(id) == 1);
			id = newId;
		}
	}
}
