/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Mark Fisher
 */
public class BoundedHashMapTests {

	@Test
	public void testCapacityEnforced() {
		BoundedHashMap<String, Integer> map = new BoundedHashMap<String, Integer>(3);
		map.put("A", 1);
		map.put("B", 2);
		map.put("C", 3);
		assertEquals(3, map.size());
		assertTrue(map.containsKey("A"));
		assertTrue(map.containsKey("B"));
		assertTrue(map.containsKey("C"));
		map.put("D", 4);
		assertEquals(3, map.size());
		assertFalse(map.containsKey("A"));
		assertTrue(map.containsKey("B"));
		assertTrue(map.containsKey("C"));
		assertTrue(map.containsKey("D"));
	}

}
