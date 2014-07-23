/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.file.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author Gary Russell
 * @since 4.0.4
 *
 */
public class AcceptOnceFileListFilterTests {

	@Test
	public void testRollback() {
		AcceptOnceFileListFilter<String> filter = new AcceptOnceFileListFilter<String>();
		doTestRollback(filter);
	}

	protected void doTestRollback(ReversibleFileListFilter<String> filter) {
		String[] files = new String[] {"foo", "bar", "baz"};
		List<String> passed = filter.filterFiles(files);
		assertTrue(Arrays.equals(files, passed.toArray()));
		List<String> now = filter.filterFiles(files);
		assertEquals(0, now.size());
		filter.rollback(passed.get(1), passed);
		now = filter.filterFiles(files);
		assertEquals(2, now.size());
		assertEquals("bar", now.get(0));
		assertEquals("baz", now.get(1));
		now = filter.filterFiles(files);
		assertEquals(0, now.size());
	}

}
